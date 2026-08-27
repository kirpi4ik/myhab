package org.myhab.demo

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Stands in for the physical ESP controllers behind the public demo.
 *
 * The server never turns a port on by itself: {@code PowerService} publishes a command
 * and then waits for the device to echo its new state back over MQTT, which is what
 * updates the database and the UI. With no hardware and nothing answering the broker,
 * every control in the demo would appear dead. This process is that answer.
 *
 * Contract (ESP model, see MQTTTopic.ESP in the server):
 * <pre>
 *   in   myhab/&lt;device&gt;/&lt;portType&gt;/&lt;portCode&gt;/cmd     ON | OFF | TOGGLE
 *   out  myhab/&lt;device&gt;/&lt;portType&gt;/&lt;portCode&gt;/state   ON | OFF | &lt;sensor value&gt;   (retained)
 *   out  myhab/&lt;device&gt;/status                          online | offline            (retained)
 * </pre>
 *
 * State topics are retained so a browser refresh, an app restart, or a reconnect all
 * re-derive the same picture without waiting for the next change.
 *
 * A device entry may carry its own {@code "prefix"} (e.g. {@code "myhab/tuya"} for
 * the TUYA dialect's sub-namespace) — it then answers on that root instead of the
 * spec-level prefix. Prefixes may contain slashes. Device codes must stay unique
 * across prefixes: live values are keyed by device/type/code alone.
 */
@Slf4j
class DeviceSimulator {

    private static final String RESET_SUFFIX = '_demo/reset'

    private final Map<String, Object> spec
    private final String prefix
    private final MqttClient client
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2)
    private final Random random = new Random()

    /** Live port values, keyed by "device/type/code". */
    private final Map<String, String> values = [:].asSynchronized()

    /** Device codes seeded as anything other than online. */
    private final Set<String> offlineDevices = [] as Set

    /** Rolling counter so demo intercom notifications are never deduped away. */
    private final java.util.concurrent.atomic.AtomicInteger notifySeq = new java.util.concurrent.atomic.AtomicInteger()

    DeviceSimulator(Map<String, Object> spec, String brokerUri, String username, String password) {
        this.spec = spec
        this.prefix = spec.prefix ?: 'myhab'
        this.client = new MqttClient(brokerUri, "myhab-demo-simulator", new MemoryPersistence())

        def options = new MqttConnectOptions(
                automaticReconnect: true,
                cleanSession: true,
                connectionTimeout: 10)
        if (username) {
            options.userName = username
            options.password = (password ?: '').toCharArray()
        }

        client.setCallback(new MqttCallbackExtended() {
            @Override
            void connectComplete(boolean reconnect, String serverURI) {
                log.info("Connected to ${serverURI}${reconnect ? ' (reconnect)' : ''}")
                // Never subscribe or publish on Paho's callback thread: publish() with
                // QoS 1 blocks waiting for the PUBACK that this very thread has to
                // deliver, so the client stalls and the broker drops it - which shows
                // up as an endless connect/disconnect loop.
                scheduler.execute {
                    try {
                        subscribe()
                        publishAll()
                    } catch (Exception e) {
                        log.error("Failed to (re)initialise after connect: ${e.message}", e)
                    }
                }
            }

            @Override
            void connectionLost(Throwable cause) {
                log.warn("Connection lost: ${cause}")
            }

            @Override
            void messageArrived(String topic, MqttMessage message) {
                // Same rule: hand off before doing anything that publishes.
                String payload = new String(message.payload, 'UTF-8')
                scheduler.execute { handleCommand(topic, payload) }
            }

            @Override
            void deliveryComplete(IMqttDeliveryToken token) {}
        })

        client.connect(options)
    }

    private void subscribe() {
        // Commands for any device/portType/portCode on every prefix in use, plus the
        // reset trigger the server calls after restoring the seed dataset.
        prefixes().each { client.subscribe("${it}/+/+/+/cmd" as String, 1) }
        client.subscribe("${prefix}/${RESET_SUFFIX}" as String, 1)
    }

    /** Seed every device and port so the UI is correct before anything is touched. */
    private void publishAll() {
        eachDevice { Map device ->
            String devPrefix = prefixOf(device)
            String status = (device.status ?: 'online') as String
            if (status != 'online') offlineDevices << (device.code as String)
            publish("${devPrefix}/${device.code}/status", status)
            eachPort(device) { Map port ->
                String key = keyOf(device.code as String, port)
                values[key] = (port.initial ?: 'OFF') as String
                publish("${devPrefix}/${key}/state", values[key])
            }
        }
    }

    private void handleCommand(String topic, String payload) {
        if (topic == "${prefix}/${RESET_SUFFIX}") {
            log.info("Reset requested - republishing seed state")
            publishAll()
            return
        }

        // <prefix>/<device>/<type>/<code>/cmd — prefixes may contain slashes
        // (myhab/tuya), so strip the longest known prefix rather than splitting
        // blindly. The prefix is echoed back on the state topic.
        String cmdPrefix = prefixes().findAll { topic.startsWith("${it}/") }
                                     .max { it.length() }
        if (cmdPrefix == null) {
            log.warn("Ignoring command on unknown prefix: ${topic}")
            return
        }
        def parts = topic.substring(cmdPrefix.length() + 1).split('/')
        if (parts.length != 4 || parts[3] != 'cmd') {
            log.warn("Ignoring unexpected command topic: ${topic}")
            return
        }
        String key = "${parts[0]}/${parts[1]}/${parts[2]}"

        // An offline device does not actuate. The UI already disables its controls,
        // but a command can still arrive over the API, and answering it would make
        // the demo contradict the status it is advertising.
        if (offlineDevices.contains(parts[0])) {
            log.info("Ignoring command for offline device ${parts[0]}")
            return
        }

        String current = values[key] ?: 'OFF'
        String next
        switch (payload?.trim()?.toUpperCase()) {
            case 'ON': next = 'ON'; break
            case 'OFF': next = 'OFF'; break
            case 'TOGGLE': next = (current == 'ON' ? 'OFF' : 'ON'); break
            default:
                log.warn("Ignoring unrecognised command '${payload}' on ${topic}")
                return
        }

        // Real hardware takes a moment to actuate and report back. Echoing instantly
        // makes the UI's pending state invisible and the demo feel fake.
        long delayMs = 200 + random.nextInt(400)
        scheduler.schedule({
            values[key] = next
            publish("${cmdPrefix}/${key}/state", next)
            log.info("${key}: ${current} -> ${next}")
        } as Runnable, delayMs, TimeUnit.MILLISECONDS)
    }

    /**
     * Random-walk the sensors so gauges and live charts move while a visitor watches.
     */
    void startSensorDrift() {
        int interval = (spec.sensorIntervalSeconds ?: 30) as int
        scheduler.scheduleAtFixedRate({
            try {
                eachDevice { Map device ->
                    if ((device.status ?: 'online') != 'online') return
                    String devPrefix = prefixOf(device)
                    eachPort(device) { Map port ->
                        if (port.kind != 'sensor') return
                        String key = keyOf(device.code as String, port)
                        publish("${devPrefix}/${key}/state", drift(port, values[key]))
                    }
                }
            } catch (Exception e) {
                log.error("Sensor drift failed: ${e.message}", e)
            }
        } as Runnable, interval, interval, TimeUnit.SECONDS)
    }

    private String drift(Map port, String current) {
        BigDecimal min = (port.min ?: 0) as BigDecimal
        BigDecimal max = (port.max ?: 100) as BigDecimal
        BigDecimal step = (port.step ?: 0.2) as BigDecimal
        int decimals = (port.decimals ?: 1) as int

        BigDecimal value
        try {
            value = new BigDecimal(current ?: (port.initial as String))
        } catch (NumberFormatException ignored) {
            value = (min + max) / 2
        }

        value = value + step * (random.nextDouble() * 2 - 1)
        if (value < min) value = min
        if (value > max) value = max

        String next = value.setScale(decimals, BigDecimal.ROUND_HALF_UP).toString()
        values[keyOf(port.deviceCode as String, port)] = next
        return next
    }

    /**
     * Periodically fire an intercom notification so the demo shows a doorbell /
     * motion message landing in the bell badge, the way the tuya bridge would from
     * a real TMEZON unit. Published on myhab/&lt;source&gt;/notify (the server NOTIFY
     * channel), non-retained, alternating doorbell and motion.
     */
    void startIntercomDemo() {
        Map intercom = spec.intercom as Map
        if (!intercom) return
        String source = (intercom.source ?: 'intercom') as String
        int interval = (intercom.intervalSeconds ?: 45) as int
        List<Map> events = [
                [subject: 'Doorbell', message: 'Someone is at the gate', key: 'doorbell'],
                [subject: 'Motion detected', message: 'Motion at the gate camera', key: 'motion'],
        ]
        scheduler.scheduleAtFixedRate({
            try {
                int seq = notifySeq.getAndIncrement()
                Map ev = events[seq % events.size()]
                // Vary the dedupKey each time so successive demo events are not
                // collapsed by the server's dedup cooldown.
                String body = groovy.json.JsonOutput.toJson([
                        subject : ev.subject, message: ev.message, level: 'INFO',
                        dedupKey: "intercom.${ev.key}.${seq}".toString(), cooldown: 1])
                publish("myhab/${source}/notify", body, false)
                log.info("intercom demo notify: ${ev.subject}")
            } catch (Exception e) {
                log.error("intercom demo notify failed: ${e.message}", e)
            }
        } as Runnable, interval, interval, TimeUnit.SECONDS)
    }

    private void publish(String topic, String payload) {
        publish(topic, payload, true)
    }

    private void publish(String topic, String payload, boolean retained) {
        def message = new MqttMessage(payload.getBytes('UTF-8'))
        message.qos = 1
        message.retained = retained
        client.publish(topic, message)
    }

    private static String keyOf(String deviceCode, Map port) {
        return "${deviceCode}/${port.type}/${port.code}"
    }

    private String prefixOf(Map device) {
        return (device.prefix ?: prefix) as String
    }

    private Set<String> prefixes() {
        Set<String> all = [prefix] as Set
        eachDevice { Map device -> all << prefixOf(device) }
        return all
    }

    private void eachDevice(Closure body) {
        (spec.devices ?: []).each { body(it as Map) }
    }

    private static void eachPort(Map device, Closure body) {
        (device.ports ?: []).each { port ->
            // Carry the owning device down so drift() can key its own writes.
            (port as Map).deviceCode = device.code
            body(port as Map)
        }
    }

    static void main(String[] args) {
        def opts = parseArgs(args)

        if (opts.embeddedBroker) {
            EmbeddedBroker.start(opts.brokerPort as int)
        }

        def specFile = new File(opts.devices as String)
        if (!specFile.exists()) {
            System.err.println("Device spec not found: ${specFile.absolutePath}")
            System.exit(1)
        }
        Map spec = new JsonSlurper().parse(specFile) as Map

        def simulator = new DeviceSimulator(spec, opts.broker as String,
                opts.username as String, opts.password as String)
        simulator.startSensorDrift()
        simulator.startIntercomDemo()

        log.info("Simulator running against ${opts.broker} with ${(spec.devices ?: []).size()} device(s)")
        Thread.currentThread().join()
    }

    private static Map parseArgs(String[] args) {
        def opts = [
                broker         : 'tcp://localhost:1883',
                brokerPort     : 1883,
                devices        : 'devices.json',
                username       : null,
                password       : null,
                embeddedBroker : false,
        ]
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case '--broker': opts.broker = args[++i]; break
                case '--broker-port': opts.brokerPort = args[++i] as int; break
                case '--devices': opts.devices = args[++i]; break
                case '--username': opts.username = args[++i]; break
                case '--password': opts.password = args[++i]; break
                case '--embedded-broker': opts.embeddedBroker = true; break
                default: System.err.println("Unknown argument: ${args[i]}"); System.exit(1)
            }
        }
        return opts
    }
}
