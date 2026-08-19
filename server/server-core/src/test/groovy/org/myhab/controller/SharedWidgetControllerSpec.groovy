package org.myhab.controller

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.json.JsonSlurper
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetAudit
import org.myhab.domain.SharedWidgetAuditResult
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.SharedWidgetType
import spock.lang.Specification

class SharedWidgetControllerSpec extends Specification
        implements ControllerUnitTest<SharedWidgetController>, DataTest {

    /** Topics captured instead of hitting the real event bus. */
    List published = []

    void setupSpec() {
        mockDomains(SharedWidget, SharedWidgetAudit)
    }

    def setup() {
        published = []
        controller.metaClass.publish = { String topic, Object data ->
            published << [topic: topic, data: data]
            return null
        }
    }

    private SharedWidget widget(Map overrides = [:]) {
        Date now = new Date()
        Map attrs = [
                token            : 'tok123',
                widgetType       : SharedWidgetType.WATER_PUMP,
                peripheralId     : '42',
                shareStartDate   : new Date(now.time - 3_600_000),
                shareExpireDate  : new Date(now.time + 3_600_000),
                actionsAllowed   : 2,
                actionsUsed      : 0,
                state            : SharedWidgetState.VALID,
                createdByUsername: 'admin'
        ] + overrides
        return new SharedWidget(attrs).save(flush: true, failOnError: true)
    }

    private void callAction(SharedWidget sw, Map body) {
        controller.params.token = sw.token
        request.method = 'POST'
        request.json = body
    }

    private Map jsonResponse() {
        return new JsonSlurper().parseText(response.text) as Map
    }

    void "switch 'on' consumes one allowance and publishes the peripheral's topic"() {
        given:
            SharedWidget sw = widget()

        when:
            callAction(sw, [action: 'on'])
            controller.executeAction()

        then:
            jsonResponse().success
            jsonResponse().actionsRemaining == 1
            sw.refresh().actionsUsed == 1
            published.size() == 1
            published[0].topic == 'evt_light'
            published[0].data.p4 == 'on'
            published[0].data.p2 == '42'
            published[0].data.p6 == 'shared:tok123'
    }

    void "switch 'off' does not consume an allowance"() {
        given:
            SharedWidget sw = widget(actionsUsed: 1)

        when:
            callAction(sw, [action: 'off'])
            controller.executeAction()

        then:
            jsonResponse().success
            sw.refresh().actionsUsed == 1
            published[0].data.p4 == 'off'
    }

    void "'off' still works once the allowance is spent and the link has expired"() {
        given:
            SharedWidget sw = widget(actionsUsed: 2, actionsAllowed: 2)

        when:
            callAction(sw, [action: 'off'])
            controller.executeAction()

        then:
            response.status == 200
            jsonResponse().success
            published[0].data.p4 == 'off'
    }

    void "'on' is refused once the allowance is spent"() {
        given:
            SharedWidget sw = widget(actionsUsed: 2, actionsAllowed: 2)

        when:
            callAction(sw, [action: 'on'])
            controller.executeAction()

        then:
            response.status == 403
            !jsonResponse().success
            published.isEmpty()
            SharedWidgetAudit.findAllByResult(SharedWidgetAuditResult.DENIED_STATE).size() == 1
    }

    void "'off' is refused when an admin disabled the link"() {
        given:
            SharedWidget sw = widget(state: SharedWidgetState.DISABLED)

        when:
            callAction(sw, [action: 'off'])
            controller.executeAction()

        then:
            response.status == 403
            published.isEmpty()
    }

    void "a wrong PIN on executeAction denies the action and audits it"() {
        given:
            SharedWidget sw = widget(pin: '4321')

        when:
            callAction(sw, [action: 'on', pin: '1111'])
            controller.executeAction()

        then:
            response.status == 403
            jsonResponse().error == 'Invalid PIN'
            published.isEmpty()
            sw.refresh().actionsUsed == 0
            SharedWidgetAudit.count() == 1
            SharedWidgetAudit.first().result == SharedWidgetAuditResult.DENIED_PIN
    }

    void "a wrong PIN on verifyPin audits a denial without touching the counter"() {
        given:
            SharedWidget sw = widget(pin: '4321')

        when:
            controller.params.token = sw.token
            request.method = 'POST'
            request.json = [pin: '0000']
            controller.verifyPin()

        then:
            response.status == 403
            sw.refresh().actionsUsed == 0
            SharedWidgetAudit.count() == 1
            SharedWidgetAudit.first().result == SharedWidgetAuditResult.DENIED_PIN
            SharedWidgetAudit.first().action == 'verify-pin'
    }

    void "a successful gate open audits one SUCCESS row carrying the forwarded client ip"() {
        given:
            SharedWidget sw = widget(widgetType: SharedWidgetType.GATE_ACCESS)

        when:
            controller.params.token = sw.token
            request.method = 'POST'
            request.addHeader('X-Forwarded-For', '203.0.113.7, 10.0.0.1')
            request.json = [:]
            controller.executeAction()

        then:
            jsonResponse().success
            published[0].topic == 'evt_intercom_door_lock'
            published[0].data.p4 == 'open'
            sw.refresh().actionsUsed == 1
            SharedWidgetAudit.count() == 1
            with(SharedWidgetAudit.first()) {
                result == SharedWidgetAuditResult.SUCCESS
                action == 'open'
                remoteAddress == '203.0.113.7'
            }
    }

    void "an unsupported action value is rejected and consumes nothing"() {
        given:
            SharedWidget sw = widget()

        when:
            callAction(sw, [action: 'rev'])
            controller.executeAction()

        then:
            response.status == 400
            published.isEmpty()
            sw.refresh().actionsUsed == 0
            SharedWidgetAudit.count() == 0
    }

    void "an unknown token is a 404 and writes no audit row"() {
        when:
            controller.params.token = 'nope'
            request.method = 'POST'
            request.json = [action: 'on']
            controller.executeAction()

        then:
            response.status == 404
            SharedWidgetAudit.count() == 0
    }
}
