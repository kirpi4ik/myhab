package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.ConfigKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.PeripheralCategory
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortValue
import spock.lang.Specification

/**
 * Auto-off deadline contract: an ON arms the deadline exactly once
 * (putIfAbsent) so repeat echoes — e.g. the periodic force-read replies —
 * cannot push it forever; only a one-shot override resets it; an OFF from any
 * transport disarms it. The same rules apply to HTTP-sync sourced events,
 * which use the async subscription.
 */
class PortValueServiceSpec extends Specification implements ServiceUnitTest<PortValueService>, DataTest {

    IMap expireMap
    Device device
    DevicePort port
    DevicePeripheral peripheral

    void setupSpec() {
        mockDomains(Device, DevicePort, DevicePeripheral, PeripheralCategory, Configuration, PortValue)
    }

    def setup() {
        expireMap = Mock(IMap)
        service.hazelcastInstance = Mock(HazelcastInstance) {
            getMap(_) >> expireMap
        }
        service.configProvider = new Expando(get: { Class type, String key -> false })

        def category = new PeripheralCategory(name: 'lighting').save(flush: true, failOnError: true)
        device = new Device(code: 'mp50', model: DeviceModel.MEGAD_2561_RTC)
        port = new DevicePort(internalRef: '12', value: 'OFF')
        device.addToPorts(port)
        device.save(flush: true, failOnError: true)
        peripheral = new DevicePeripheral(name: 'hall light', category: category)
        peripheral.addToConnectedTo(port)
        peripheral.save(flush: true, failOnError: true)
    }

    private void timeoutConfig(String seconds) {
        new Configuration(entityType: EntityType.PERIPHERAL, entityId: peripheral.id,
                key: ConfigKey.STATE_ON_TIMEOUT, value: seconds).save(flush: true, failOnError: true)
    }

    private void overrideConfig(String seconds) {
        new Configuration(entityType: EntityType.PERIPHERAL, entityId: peripheral.id,
                key: ConfigKey.STATE_ON_TIMEOUT_OVERRIDE, value: seconds).save(flush: true, failOnError: true)
    }

    private static Map onEvent(String source = 'mqtt') {
        [data: [p2: 'mp50', p4: '12', p5: 'ON', p6: source]]
    }

    void "an ON arms the deadline via putIfAbsent so repeat echoes cannot extend it"() {
        given:
            timeoutConfig('30')

        when:
            service.updateExpirationTime(onEvent())

        then:
            1 * expireMap.putIfAbsent(String.valueOf(port.id),
                    { it.peripheralId == peripheral.id && it.expireOn > System.currentTimeMillis() })
            0 * expireMap.put(*_)
    }

    void "an ON with a one-shot override resets the deadline and consumes the row"() {
        given:
            timeoutConfig('3600')
            overrideConfig('5')

        when:
            service.updateExpirationTime(onEvent())

        then:
            1 * expireMap.put(String.valueOf(port.id), { it.peripheralId == peripheral.id })
            0 * expireMap.putIfAbsent(*_)
            // countByKey sees a stale session cache in the map datastore; list() is live
            Configuration.list().every { it.key != ConfigKey.STATE_ON_TIMEOUT_OVERRIDE }
    }

    void "an OFF disarms the deadline and clears a stale override"() {
        given:
            overrideConfig('5')

        when:
            service.updateExpirationTime([data: [p2: 'mp50', p4: '12', p5: 'OFF', p6: 'mqtt']])

        then:
            1 * expireMap.remove(String.valueOf(port.id))
            0 * expireMap.put(*_)
            0 * expireMap.putIfAbsent(*_)
            Configuration.list().every { it.key != ConfigKey.STATE_ON_TIMEOUT_OVERRIDE }
    }

    void "an HTTP-sync sourced ON arms the deadline exactly like an MQTT echo"() {
        given:
            timeoutConfig('30')

        when:
            service.updateExpirationTimeAsync(onEvent('device http sync job'))

        then:
            1 * expireMap.putIfAbsent(String.valueOf(port.id), { it.peripheralId == peripheral.id })
            0 * expireMap.put(*_)
    }

    void "a peripheral without a timeout configuration arms nothing"() {
        when:
            service.updateExpirationTime(onEvent())

        then:
            0 * expireMap.put(*_)
            0 * expireMap.putIfAbsent(*_)
    }
}
