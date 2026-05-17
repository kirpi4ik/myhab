package org.myhab.async.mqtt

import grails.events.annotation.Subscriber
import groovy.util.logging.Slf4j
import org.eclipse.paho.client.mqttv3.IMqttMessageListener
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Provides ad-hoc access to the latest value seen for an MQTT topic, used by
 * the UI to populate fields (like a device's current IP) when the regular
 * {@code DevicePort}-based persistence path hasn't captured the value.
 *
 * <p>Two-tier lookup:</p>
 * <ol>
 *   <li><b>In-memory cache</b> — every {@code evt_mqtt_port_value_changed}
 *       event the existing inbound MQTT channel publishes is recorded here,
 *       keyed by {@code (deviceCode, portCode)}. Cache hits answer instantly
 *       and survive cases where downstream persistence fails (e.g. per-device
 *       autoimport not enabled, schema mismatch, etc).</li>
 *   <li><b>Live broker subscribe</b> — on a cache miss, spin up a transient
 *       {@link MqttClient}, subscribe to the requested topic, and wait briefly
 *       for a payload (retained messages arrive immediately on subscribe;
 *       non-retained ones arrive whenever the publisher next sends within the
 *       timeout). The transient client is always disconnected and closed in
 *       a finally block to avoid leaking broker connections.</li>
 * </ol>
 */
@Slf4j
class MqttRetainedFetchService {

    def configProvider

    /** Cache key = "deviceCode|portCode" -> latest payload string. */
    private final Map<String, String> cache = new ConcurrentHashMap<>()

    /**
     * Cache every incoming MQTT port-value event so subsequent UI lookups
     * don't need to talk to the broker. Runs alongside the regular
     * {@code PortValueService.updatePort} subscriber.
     */
    @Subscriber('evt_mqtt_port_value_changed')
    void cacheIncomingMqttValue(event) {
        try {
            String deviceCode = event?.data?.p2 as String
            String portCode = event?.data?.p4 as String
            String value = event?.data?.p5 as String
            if (deviceCode && portCode && value != null) {
                cache.put(cacheKey(deviceCode, portCode), value)
            }
        } catch (Exception ex) {
            log.warn("MqttRetainedFetchService cache update failed: ${ex.message}")
        }
    }

    /**
     * Read the latest cached value for a device/port pair, or {@code null}
     * if the backend hasn't observed a message for it since startup. Pair
     * with {@link #fetchLatestValue} for a broker fallback when this misses.
     */
    String getCachedValue(String deviceCode, String portCode) {
        if (!deviceCode || !portCode) return null
        return cache.get(cacheKey(deviceCode, portCode))
    }

    /**
     * Fetch the latest payload published to {@code topic} or {@code null} if
     * nothing arrives within {@code timeoutMs}. Used as a fallback when the
     * in-memory cache misses (e.g. right after a backend restart, before the
     * device's next heartbeat).
     *
     * @param topic full MQTT topic (no wildcards)
     * @param timeoutMs how long to wait, in milliseconds (sane range: 500–15000)
     * @return the payload as a UTF-8 string, or {@code null} on timeout / error
     */
    String fetchLatestValue(String topic, long timeoutMs = 5000L) {
        if (!topic) {
            log.warn("fetchLatestValue called with blank topic")
            return null
        }

        String brokerHost = configProvider.get(String.class, "mqtt.hostname")
        String brokerPort = configProvider.get(String.class, "mqtt.port")
        String username = configProvider.get(String.class, "mqtt.username")
        String password = configProvider.get(String.class, "mqtt.password")
        if (!brokerHost || !brokerPort) {
            log.error("Cannot fetch MQTT value: broker hostname/port not configured")
            return null
        }

        String serverUri = "tcp://${brokerHost}:${brokerPort}"
        String clientId = "myhab-fetch-${UUID.randomUUID().toString().replace('-', '').take(16)}"
        MqttClient client = null
        long started = System.currentTimeMillis()
        log.info("MQTT fetch: connecting to ${serverUri} as ${clientId} to read topic ${topic} (timeout=${timeoutMs}ms)")
        try {
            client = new MqttClient(serverUri, clientId, null /* in-memory persistence */)
            MqttConnectOptions opts = new MqttConnectOptions()
            opts.setCleanSession(true)
            opts.setAutomaticReconnect(false)
            opts.setConnectionTimeout(10) // seconds for the TCP/MQTT handshake itself
            if (username) opts.setUserName(username)
            if (password) opts.setPassword(password.toCharArray())
            client.connect(opts)
            log.debug("MQTT fetch: connected in ${System.currentTimeMillis() - started}ms")

            CompletableFuture<String> received = new CompletableFuture<>()
            // Subscribe at QoS 0 so we accept whatever the publisher used (broker
            // delivers at the lower of subscriber and publisher QoS).
            client.subscribe(topic, 0 as int, { String t, org.eclipse.paho.client.mqttv3.MqttMessage m ->
                try {
                    String payload = new String(m.payload, "UTF-8")
                    log.debug("MQTT fetch: received ${payload.length()} bytes on ${t}")
                    received.complete(payload)
                } catch (Exception ex) {
                    received.completeExceptionally(ex)
                }
            } as IMqttMessageListener)
            log.debug("MQTT fetch: subscribed to ${topic}")

            try {
                String value = received.get(timeoutMs, TimeUnit.MILLISECONDS)
                log.info("MQTT fetch: got value for ${topic} in ${System.currentTimeMillis() - started}ms")
                return value
            } catch (TimeoutException ignored) {
                log.info("MQTT fetch: no payload on ${topic} within ${timeoutMs}ms (broker has no retained value, no recent publish)")
                return null
            }
        } catch (Exception ex) {
            log.error("MQTT fetch failed for topic ${topic}: ${ex.message}", ex)
            return null
        } finally {
            try {
                if (client?.isConnected()) {
                    client.disconnect()
                }
                client?.close()
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }
    }

    private static String cacheKey(String deviceCode, String portCode) {
        return "${deviceCode}|${portCode}"
    }
}
