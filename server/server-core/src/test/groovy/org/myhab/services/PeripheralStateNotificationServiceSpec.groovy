package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.MessageLevel
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.PeripheralCategory
import org.myhab.domain.device.port.DevicePort
import spock.lang.Specification

/**
 * Contract for state-change notifications: a confirmed ON/OFF on a watched category
 * notifies admins once with a mutable dedup key, anything else stays silent, and no
 * failure escapes onto the port-value pipeline this subscriber runs on.
 */
class PeripheralStateNotificationServiceSpec extends Specification
        implements ServiceUnitTest<PeripheralStateNotificationService>, DataTest {

    Device device
    DevicePort valvePort
    DevicePort lightPort

    void setupSpec() {
        mockDomains(Device, DevicePort, DevicePeripheral, PeripheralCategory)
    }

    def setup() {
        service.notificationService = Mock(NotificationService)

        device = new Device(code: 'water_main_valve', model: DeviceModel.TUYA)
        valvePort = new DevicePort(internalRef: 'valve', value: 'OFF')
        lightPort = new DevicePort(internalRef: 'd0', value: 'OFF')
        device.addToPorts(valvePort)
        device.addToPorts(lightPort)
        device.save(flush: true, failOnError: true)

        peripheral('Main water valve', 'VALVE', valvePort)
        peripheral('Hall light', 'LIGHT', lightPort)
    }

    private void peripheral(String name, String categoryName, DevicePort port) {
        def category = PeripheralCategory.findByName(categoryName) ?:
                new PeripheralCategory(name: categoryName).save(flush: true, failOnError: true)
        def p = new DevicePeripheral(name: name, category: category).save(flush: true, failOnError: true)
        // Wired from the port side: the service reads DevicePort.peripherals.
        port.addToPeripherals(p)
        port.save(flush: true, failOnError: true)
    }

    private Map event(DevicePort port, String value) {
        [data: [p1: 'PORT', p2: "${port.id}", p3: port.internalRef, p4: value, p6: 'mqtt']]
    }

    void "a confirmed ON on a valve notifies admins"() {
        when:
            service.portValuePersisted(event(valvePort, 'ON'))

        then:
            1 * service.notificationService.notifyAdmins(MessageLevel.INFO, 'Main water valve opened',
                    'Main water valve is now OPEN.', 'valve',
                    "peripheral.${DevicePeripheral.findByName('Main water valve').id}.on",
                    PeripheralStateNotificationService.COOLDOWN_MINUTES)
    }

    void "a confirmed OFF on a valve notifies admins with its own dedup key"() {
        when:
            service.portValuePersisted(event(valvePort, 'OFF'))

        then:
            // A distinct key per direction: an open must never be muted by the close
            // that preceded it inside the cooldown window.
            1 * service.notificationService.notifyAdmins(MessageLevel.INFO, 'Main water valve closed',
                    'Main water valve is now CLOSED.', 'valve', { it ==~ /peripheral\.\d+\.off/ }, _)
    }

    void "a peripheral outside the watched categories stays silent"() {
        when:
            service.portValuePersisted(event(lightPort, 'ON'))

        then:
            0 * service.notificationService.notifyAdmins(*_)
    }

    void "a sensor reading is not a state change"() {
        when:
            service.portValuePersisted(event(valvePort, '21.5'))

        then:
            0 * service.notificationService.notifyAdmins(*_)
    }

    void "an event for an unknown port is dropped without throwing"() {
        when:
            service.portValuePersisted([data: [p2: '999999', p4: 'ON']])

        then:
            0 * service.notificationService.notifyAdmins(*_)
            noExceptionThrown()
    }

    void "a malformed event cannot break the port-value pipeline"() {
        when:
            service.portValuePersisted([data: [p2: 'not-a-number', p4: 'ON']])

        then:
            0 * service.notificationService.notifyAdmins(*_)
            noExceptionThrown()
    }
}
