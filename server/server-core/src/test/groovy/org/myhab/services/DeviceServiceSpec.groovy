package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.Configuration
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.port.DevicePort
import spock.lang.Specification

/**
 * {@code importPort}/{@code importDevice} declare a single-entity return type but
 * query with {@code withCriteria}, which yields a List. Groovy casts a List to the
 * declared type by invoking its constructor, so an empty result silently became a
 * transient, null-id entity that passed every {@code != null} check downstream —
 * PortValueService then wrote {@code port_values} rows with a null portId and blew
 * up on the non-nullable {@code EventData.p2} audit column.
 */
class DeviceServiceSpec extends Specification implements ServiceUnitTest<DeviceService>, DataTest {

    void setupSpec() {
        mockDomains(Device, DevicePort, Configuration)
    }

    void setup() {
        // Auto-import off: exercises the lookup-miss path only.
        service.configProvider = new Expando(get: { Class type, String key -> false })
    }

    void "importPort returns null when the port is unknown"() {
        given:
            Device device = new Device(code: 'esp-01', model: DeviceModel.ESP32).save(flush: true)

        when:
            DevicePort port = service.importPort(device, 'RELAY', 'p99')

        then:
            port == null
    }

    void "importPort returns the persisted port when it exists"() {
        given:
            Device device = new Device(code: 'esp-02', model: DeviceModel.ESP32)
            device.addToPorts(new DevicePort(internalRef: 'p1'))
            device.save(flush: true)

        when:
            DevicePort port = service.importPort(device, 'RELAY', 'p1')

        then:
            port != null
            port.id == device.ports.first().id
            port.internalRef == 'p1'
    }

    void "importDevice returns null when the device is unknown"() {
        expect:
            service.importDevice(DeviceModel.ESP32.name(), 'nope') == null
    }

    void "importDevice returns the persisted device when it exists"() {
        given:
            Device device = new Device(code: 'esp-03', model: DeviceModel.ESP32).save(flush: true)

        when:
            Device found = service.importDevice(DeviceModel.ESP32.name(), 'esp-03')

        then:
            found?.id == device.id
    }
}
