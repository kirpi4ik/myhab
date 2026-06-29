package org.myhab.services.audit

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.AuditSource
import org.myhab.domain.job.EventData
import spock.lang.Specification

class AuditServiceSpec extends Specification implements ServiceUnitTest<AuditService>, DataTest {

    void setupSpec() {
        mockDomain(EventData)
    }

    void "logStateChange persists exactly one row with normalized fields"() {
        when:
            EventData ev = service.logStateChange(EntityType.PORT, 2239L, PortAction.ON,
                    AuditSource.SCENARIO, 'CRON', [peripheralId: 5L])

        then:
            EventData.count() == 1
            ev.p1 == 'PORT'
            ev.p2 == '2239'
            ev.p3 == 'SCENARIO'
            ev.p4 == 'ON'
            ev.p6 == 'CRON'
            ev.p5.contains('peripheralId')
    }

    void "null actor falls back to SYSTEM and empty details produce null p5"() {
        when:
            EventData ev = service.logStateChange(EntityType.PERIPHERAL, 7L, PortAction.OFF,
                    AuditSource.WEB_UI, null, [:])

        then:
            ev.p3 == 'WEB_UI'
            ev.p4 == 'OFF'
            ev.p6 == 'SYSTEM'
            ev.p5 == null
    }

    void "actionId in details is persisted to the column, not into p5"() {
        when:
            EventData ev = service.logStateChange(EntityType.PORT, 5L, PortAction.ON,
                    AuditSource.SCENARIO, 'CRON', [actionId: 'act-123', peripheralId: 9L])

        then:
            ev.actionId == 'act-123'
            ev.p5.contains('peripheralId')
            !ev.p5.contains('act-123')
            !ev.p5.contains('actionId')
    }

    void "log() records a non-PortAction event under the given topic"() {
        when:
            EventData ev = service.log('evt_intercom_door_lock', EntityType.PERIPHERAL, 42L,
                    'open', AuditSource.ACCESS_CONTROL, 'alice', [unlockCode: '1234'])

        then:
            EventData.count() == 1
            ev.p0 == 'evt_intercom_door_lock'
            ev.p2 == '42'
            ev.p4 == 'open'
            ev.p6 == 'alice'
    }
}
