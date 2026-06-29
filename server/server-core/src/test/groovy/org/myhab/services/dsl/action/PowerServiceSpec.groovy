package org.myhab.services.dsl.action

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.AuditSource
import org.myhab.services.audit.AuditService
import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * PowerService is now the single place audit rows are written for commanded
 * port actions — reflecting reality (after a successful MQTT publish), per
 * resolved port. DataTest supplies the transaction manager the readOnly service
 * needs; the GORM finder is stubbed so the test does not depend on schema.
 */
class PowerServiceSpec extends Specification implements ServiceUnitTest<PowerService>, DataTest {

    void setupSpec() {
        mockDomain(DevicePort)
    }

    IMap pendingMap

    def setup() {
        service.mqttTopicService = Mock(MqttTopicService)
        service.auditService = Mock(AuditService)
        pendingMap = Mock(IMap)
        service.hazelcastInstance = Mock(HazelcastInstance) {
            getMap(_) >> pendingMap
        }
    }

    def cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(DevicePort)
    }

    private static DevicePort port(Long id) {
        DevicePort p = new DevicePort(internalRef: "p-${id}", name: "port-${id}")
        p.id = id
        return p
    }

    private void stubLookup(Map<Long, DevicePort> byId) {
        DevicePort.metaClass.static.findAllByIdInList = { List ids ->
            ids.collect { byId[it as Long] }.findAll { it != null }
        }
    }

    void "a resolved port is audited once after a successful MQTT publish"() {
        given:
            DevicePort p = port(2239L)
            stubLookup([2239L: p])

        when:
            service.execute([portIds: [2239L], action: PortAction.ON,
                             source: AuditSource.SCENARIO, actor: 'CRON'])

        then:
            1 * service.mqttTopicService.publish(_, [PortAction.ON])
            1 * pendingMap.put('2239:ON', { it.actionId != null }, 30, TimeUnit.SECONDS)
            1 * service.auditService.logStateChange(EntityType.PORT, 2239L, PortAction.ON,
                    AuditSource.SCENARIO, 'CRON', { it.actionId != null })
    }

    void "an unresolved port id produces no MQTT publish and no audit row"() {
        given:
            stubLookup([:])

        when:
            service.execute([portIds: [9999999L], action: PortAction.ON,
                             source: AuditSource.SCENARIO, actor: 'CRON'])

        then:
            0 * service.mqttTopicService.publish(_, _)
            0 * pendingMap.put(*_)
            0 * service.auditService.logStateChange(*_)
    }

    void "an MQTT failure is recorded as a FAILED audit row"() {
        given:
            DevicePort p = port(1L)
            stubLookup([1L: p])

        when:
            service.execute([portIds: [1L], action: PortAction.ON,
                             source: AuditSource.SCENARIO, actor: 'CRON'])

        then:
            1 * service.mqttTopicService.publish(_, _) >> { throw new RuntimeException('broker down') }
            0 * pendingMap.put(*_)
            1 * service.auditService.logStateChange(EntityType.PORT, 1L, PortAction.ON,
                    AuditSource.SCENARIO, 'CRON', { it.status == 'FAILED' && it.actionId != null })
    }
}
