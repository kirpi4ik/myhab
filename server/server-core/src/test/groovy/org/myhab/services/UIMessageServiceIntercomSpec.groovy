package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.config.ConfigProvider
import org.myhab.services.audit.AuditService
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.PeripheralCategory
import org.myhab.domain.device.port.DevicePort
import spock.lang.Specification

/**
 * The intercom unlock path is separate from the DOOR_LOCK relay and PIN-gated:
 * a wrong PIN never reaches the device, only an INTERCOM-category peripheral is
 * accepted, and the relay path (doorOpen) is never touched here.
 */
class UIMessageServiceIntercomSpec extends Specification
        implements ServiceUnitTest<UIMessageService>, DataTest {

    DevicePeripheral intercom
    Device device

    void setupSpec() {
        mockDomains(Device, DevicePort, DevicePeripheral, PeripheralCategory)
    }

    def setup() {
        service.intercomService = Mock(IntercomService)
        service.auditService = Mock(AuditService)
        service.configProvider = Mock(ConfigProvider)

        device = new Device(code: 'intercom', model: DeviceModel.TMEZON_INTERCOM)
        DevicePort lockPort = new DevicePort(internalRef: 'lock', value: 'OFF')
        device.addToPorts(lockPort)
        device.save(flush: true, failOnError: true)

        intercom = peripheral('Gate Intercom', 'INTERCOM', lockPort)
    }

    private DevicePeripheral peripheral(String name, String categoryName, DevicePort port) {
        def category = new PeripheralCategory(name: categoryName).save(flush: true, failOnError: true)
        def p = new DevicePeripheral(name: name, category: category)
        p.addToConnectedTo(port)
        p.save(flush: true, failOnError: true)
        return p
    }

    private Map unlockEvent(Long peripheralId, String pin) {
        [data: [p1: 'PERIPHERAL', p2: "${peripheralId}", p3: 'mweb', p4: 'unlock', p5: pin, p6: 'demo']]
    }

    void "a correct PIN unlocks the intercom"() {
        given:
            service.configProvider.get(String.class, 'intercom.intercom.unlockPin') >> '1234'

        when:
            service.evt_intercom_unlock(unlockEvent(intercom.id, '1234'))

        then:
            1 * service.intercomService.unlockIntercom({ it.code == 'intercom' })
    }

    void "a wrong PIN is rejected and never reaches the device"() {
        given:
            service.configProvider.get(String.class, 'intercom.intercom.unlockPin') >> '1234'

        when:
            service.evt_intercom_unlock(unlockEvent(intercom.id, '0000'))

        then:
            0 * service.intercomService.unlockIntercom(_)
    }

    void "with no PIN configured the unlock proceeds"() {
        given:
            service.configProvider.get(String.class, 'intercom.intercom.unlockPin') >> null

        when:
            service.evt_intercom_unlock(unlockEvent(intercom.id, ''))

        then:
            1 * service.intercomService.unlockIntercom(_)
    }

    void "a non-INTERCOM peripheral is ignored"() {
        given:
            DevicePort p = new DevicePort(internalRef: 'd0', value: 'OFF')
            device.addToPorts(p)
            device.save(flush: true, failOnError: true)
            DevicePeripheral light = peripheral('Hall light', 'LIGHT', p)

        when:
            service.evt_intercom_unlock(unlockEvent(light.id, '1234'))

        then:
            0 * service.intercomService.unlockIntercom(_)
    }
}
