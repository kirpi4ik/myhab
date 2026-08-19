package org.myhab.jobs

import grails.testing.gorm.DataTest
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.services.DeviceService
import spock.lang.Specification

/**
 * ?cmd=all reports a value for EVERY port index of the controller, while the DB
 * only holds rows for configured ports. The sync loop must skip unmapped
 * indexes and isolate per-port failures — historically the first unmapped
 * index aborted the whole device's sync, silently disabling the HTTP
 * ground-truth reconciliation.
 */
class DeviceControllerStateSyncJobSpec extends Specification implements DataTest {

    DeviceControllerStateSyncJob job
    List published

    void setupSpec() {
        mockDomains(Device, DevicePort, Configuration)
    }

    def setup() {
        job = new DeviceControllerStateSyncJob()
        job.deviceService = Mock(DeviceService)
        published = []
        job.metaClass.publish = { String ns, Object d -> published << [topic: ns, data: d] }
    }

    private Device controller(String code, Map<String, String> portValuesByRef,
                              String httpSyncCfg = 'true', DeviceModel model = DeviceModel.MEGAD_2561_RTC) {
        Device device = new Device(code: code, model: model, status: DeviceStatus.ONLINE)
        portValuesByRef.each { ref, val ->
            device.addToPorts(new DevicePort(internalRef: ref, value: val))
        }
        device.save(flush: true, failOnError: true)
        if (httpSyncCfg != null) {
            new Configuration(entityType: EntityType.DEVICE, entityId: device.id,
                    key: CfgKey.DEVICE.DEVICE_HTTP_SYNC_SUPPORTED.key(), value: httpSyncCfg)
                    .save(flush: true, failOnError: true)
        }
        return device
    }

    private Device megad(String code, Map<String, String> portValuesByRef) {
        controller(code, portValuesByRef)
    }

    void "unmapped ?cmd=all indexes are skipped and the mismatched port still publishes"() {
        given: 'a device whose DB only knows port 3, currently OFF'
            megad('mp50', ['3': 'OFF'])

        when:
            job.execute(null)

        then: 'indexes 0..4 reported, only port 3 exists and mismatches'
            1 * job.deviceService.readPortValuesFromDevice({ it.code == 'mp50' }) >>
                    ['0': 'OFF', '1': 'ON', '2': 'OFF/7', '3': 'ON', '4': 'OFF']
            published.size() == 1
            published[0].data.p2 == 'mp50'
            published[0].data.p4 == '3'
            published[0].data.p5 == 'ON'
    }

    void "a matching hardware value publishes nothing"() {
        given:
            megad('mp51', ['3': 'ON'])

        when:
            job.execute(null)

        then:
            1 * job.deviceService.readPortValuesFromDevice(_) >> ['0': 'OFF', '3': 'ON']
            published.isEmpty()
    }

    void "a MegaD without the http-sync configuration row is polled by default"() {
        given:
            controller('mp52', ['3': 'OFF'], null)

        when:
            job.execute(null)

        then:
            1 * job.deviceService.readPortValuesFromDevice({ it.code == 'mp52' }) >> [:]
    }

    void "a MegaD with an explicit false row is not polled"() {
        given:
            controller('mp54', ['3': 'OFF'], 'false')

        when:
            job.execute(null)

        then:
            0 * job.deviceService.readPortValuesFromDevice(_)
            published.isEmpty()
    }

    void "a non-MegaD device without the http-sync configuration row is not polled"() {
        given:
            controller('esp-9', ['3': 'OFF'], null, DeviceModel.ESP32)

        when:
            job.execute(null)

        then:
            0 * job.deviceService.readPortValuesFromDevice(_)
            published.isEmpty()
    }

    void "an unreachable device is logged and does not break the job"() {
        given:
            megad('mp53', ['3': 'OFF'])

        when:
            job.execute(null)

        then:
            1 * job.deviceService.readPortValuesFromDevice(_) >>
                    { throw new UnavailableDeviceException('connect timed out') }
            noExceptionThrown()
            published.isEmpty()
    }
}
