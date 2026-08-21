package org.myhab.controller

import grails.testing.web.controllers.ControllerUnitTest
import org.myhab.domain.DomainUtil
import org.myhab.services.ClientIpService
import spock.lang.Specification

class EventControllerSpec extends Specification implements ControllerUnitTest<EventController> {

    private static final String PROXY = '192.168.1.200'

    /** The product trusts no proxy by default; these cases are about a configured one. */
    Closure doWithConfig() {
        return { config ->
            config.myhab = [security: [trustedProxies: [PROXY + '/32']]]
        }
    }

    /** Topics captured instead of hitting the real event bus. */
    List published = []

    def setup() {
        published = []
        controller.metaClass.publish = { String topic, Object data ->
            published << [topic: topic, data: data]
            return null
        }
        controller.clientIpService = new ClientIpService()
        request.method = 'GET'
    }

    private void from(String peer, Map headers = [:]) {
        request.remoteAddr = peer
        headers.each { k, v -> request.addHeader(k as String, v as String) }
    }

    void "a public caller is refused and publishes nothing"() {
        given:
            from(PROXY, ['X-Real-IP': '203.0.113.7'])
            controller.params.p0 = 'evt_intercom_door_lock'
            controller.params.p1 = 'PERIPHERAL'
            controller.params.p2 = '42'
            controller.params.p4 = 'open'

        when:
            controller.pubGetEvent()

        then:
            response.status == 403
            published.isEmpty()
    }

    void "a spoofed X-Forwarded-For does not buy access"() {
        given:
            from(PROXY, [
                    'X-Forwarded-For': '192.168.1.5, 203.0.113.7',
                    'X-Real-IP'      : '203.0.113.7'
            ])
            controller.params.p0 = 'evt_light'
            controller.params.p1 = 'PORT'
            controller.params.p2 = '7'
            controller.params.p4 = 'on'

        when:
            controller.pubGetEvent()

        then:
            response.status == 403
            published.isEmpty()
    }

    void "the mdid push branch is refused from a public caller too"() {
        given:
            from(PROXY, ['X-Real-IP': '203.0.113.7'])
            controller.params.mdid = 'mega01'
            controller.params.pt = '3'

        when:
            controller.pubGetEvent()

        then:
            response.status == 403
            published.isEmpty()
    }

    void "a LAN caller through the proxy publishes the topic and the log event"() {
        given:
            from(PROXY, ['X-Real-IP': '192.168.1.44'])
            controller.params.p0 = 'evt_light'
            controller.params.p1 = 'PORT'
            controller.params.p2 = '7'
            controller.params.p4 = 'on'

        when:
            controller.pubGetEvent()

        then:
            published*.topic == ['evt_light', 'evt_log']
            published[0].data.p2 == '7'
            published[0].data.p4 == 'on'
    }

    void "a LAN device hitting the backend directly still publishes"() {
        given:
            from('192.168.1.44')
            controller.params.mdid = 'mega01'
            controller.params.pt = '3'

        when:
            controller.pubGetEvent()

        then:
            published*.topic == ['evt_device_push']
    }

    void "binding is limited to event fields so tsCreated cannot be forged"() {
        given:
            from('192.168.1.44')
            controller.params.p0 = 'evt_light'
            controller.params.p2 = '7'
            controller.params.tsCreated = '2001-01-01T00:00:00Z'

        when:
            controller.pubGetEvent()

        then: 'the sentinel survives, so beforeInsert still stamps the real time'
            published[0].data.tsCreated == DomainUtil.NULL_DATE
    }

    void "shortUrlEvent is gone"() {
        expect:
            !controller.class.methods*.name.contains('shortUrlEvent')
    }
}
