package org.myhab.services.dsl

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.AuditSource
import org.myhab.services.dsl.action.PowerService
import spock.lang.Specification

/**
 * Guards the post-redesign contract: the switch DSL methods delegate to
 * PowerService once (no separate intent-based event publish), carrying the
 * structured source + the per-thread trigger actor.
 *
 * DataTest is implemented only to initialize GORM/the transaction manager that
 * the {@code @Transactional} service requires — no domains are persisted here.
 */
class ScenarioServiceSpec extends Specification implements ServiceUnitTest<ScenarioService>, DataTest {

    def setup() {
        service.powerService = Mock(PowerService)
    }

    void "switchOn delegates once to PowerService with SCENARIO source and the thread actor"() {
        given:
            service.setExecutionActor('CRON')
            Map captured = null

        when:
            service.switchOn([portIds: [2239]])

        then:
            1 * service.powerService.execute(_) >> { args -> captured = args[0] }
            captured.topic == 'switch_on'
            captured.action == PortAction.ON
            captured.source == AuditSource.SCENARIO
            captured.actor == 'CRON'

        cleanup:
            service.clearExecutionActor()
    }

    void "switchOff and switchToggle carry the right action and actor"() {
        given:
            service.setExecutionActor('alice')
            List<Map> seen = []

        when:
            service.switchOff([portIds: [1]])
            service.switchToggle([peripheralIds: [2]])

        then:
            2 * service.powerService.execute(_) >> { args -> seen << args[0] }
            seen[0].action == PortAction.OFF
            seen[0].source == AuditSource.SCENARIO
            seen[0].actor == 'alice'
            seen[1].action == PortAction.TOGGLE
            seen[1].actor == 'alice'

        cleanup:
            service.clearExecutionActor()
    }

    void "actor defaults to SYSTEM when no execution actor is set on the thread"() {
        given:
            service.clearExecutionActor()
            Map captured = null

        when:
            service.switchOn([portIds: [9]])

        then:
            1 * service.powerService.execute(_) >> { args -> captured = args[0] }
            captured.actor == 'SYSTEM'
    }
}
