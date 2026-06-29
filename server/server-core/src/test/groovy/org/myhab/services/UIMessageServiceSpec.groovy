package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.AuditSource
import org.myhab.services.dsl.action.PowerService
import spock.lang.Specification

/**
 * Phase 1 contract: switch/light events resolve to ports and drive PowerService
 * with a structured source + actor (PowerService now writes the audit rows), and
 * the handler no longer double-logs a peripheral-scoped intent row to evt_log.
 */
class UIMessageServiceSpec extends Specification implements ServiceUnitTest<UIMessageService>, DataTest {

    List<String> published

    def setup() {
        service.powerService = Mock(PowerService)
        published = []
        service.metaClass.publish = { String ns, Object d -> published << ns }
    }

    void "a PORT switch event drives PowerService with mapped WEB_UI source + actor and no evt_log publish"() {
        given:
            def event = [data: [p1: 'PORT', p2: '5', p4: 'on', p3: 'mweb', p6: 'alice']]

        when:
            service.receiveSwitchEvent(event)

        then:
            1 * service.powerService.execute([portIds: [5L], action: PortAction.ON,
                                              source: AuditSource.WEB_UI, actor: 'alice'])
            !published.contains('evt_log')
    }

    void "a voice-sourced event maps to AuditSource.VOICE and keeps the actor"() {
        given:
            def event = [data: [p1: 'PORT', p2: '9', p4: 'off', p3: 'Voice assistant: bob', p6: 'bob']]

        when:
            service.receiveSwitchEvent(event)

        then:
            1 * service.powerService.execute([portIds: [9L], action: PortAction.OFF,
                                              source: AuditSource.VOICE, actor: 'bob'])
    }

    void "an unknown action is rejected and PowerService is not called"() {
        given:
            def event = [data: [p1: 'PORT', p2: '5', p4: 'bogus', p3: 'mweb', p6: 'alice']]

        when:
            service.receiveSwitchEvent(event)

        then:
            0 * service.powerService.execute(_)
    }
}
