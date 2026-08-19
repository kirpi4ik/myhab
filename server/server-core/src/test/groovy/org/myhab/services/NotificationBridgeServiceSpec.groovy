package org.myhab.services

import grails.testing.services.ServiceUnitTest
import org.myhab.domain.MessageLevel
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Contract for the MQTT -> in-app notification bridge: a well-formed envelope fans out
 * to admins with the topic-derived sender and a source-namespaced dedup key, and no
 * malformed payload may escape as an exception (the subscriber runs on the MQTT
 * ingestion thread, so a throw would take down the pipeline).
 */
class NotificationBridgeServiceSpec extends Specification implements ServiceUnitTest<NotificationBridgeService> {

    def setup() {
        service.notificationService = Mock(NotificationService)
    }

    private static Map event(String payload, String source = 'gate_controller') {
        [data: [p2: source, p5: payload]]
    }

    void "a well-formed envelope notifies admins with the topic-derived sender and namespaced dedup key"() {
        given:
            def payload = '{"level":"WARN","subject":"Gate access denied",' +
                    '"message":"Unknown card (99)","dedupKey":"tag.99","cooldown":5}'

        when:
            service.onMqttNotification(event(payload))

        then:
            1 * service.notificationService.notifyAdmins(MessageLevel.WARN, 'Gate access denied',
                    'Unknown card (99)', 'gate_controller', 'mqtt.gate_controller.tag.99', 5)
    }

    void "an absent message body falls back to the subject"() {
        // UserMessage.message is blank:false — an empty body would fail validation.
        when:
            service.onMqttNotification(event('{"subject":"Gate opened"}'))

        then:
            1 * service.notificationService.notifyAdmins(MessageLevel.INFO, 'Gate opened',
                    'Gate opened', 'gate_controller', null, NotificationService.DEFAULT_COOLDOWN_MINUTES)
    }

    void "an unknown level degrades to INFO instead of throwing"() {
        when:
            service.onMqttNotification(event('{"subject":"x","level":"BANANAS"}'))

        then:
            1 * service.notificationService.notifyAdmins(MessageLevel.INFO, 'x', 'x', _, _, _)
    }

    void "the cooldown is clamped to a sane window"() {
        when:
            service.onMqttNotification(event('{"subject":"x","dedupKey":"k","cooldown":99999}'))

        then:
            1 * service.notificationService.notifyAdmins(_, _, _, _, 'mqtt.gate_controller.k', 1440)
    }

    @Unroll
    void "a #description payload is dropped without notifying and without throwing"() {
        when:
            service.onMqttNotification(event(payload))

        then:
            0 * service.notificationService.notifyAdmins(*_)
            noExceptionThrown()

        where:
            description        | payload
            'null'             | null
            'empty'            | ''
            'non-JSON'         | 'not-json'
            'JSON array'       | '[]'
            'JSON scalar'      | '"just a string"'
            'subject-less'     | '{"message":"orphan"}'
            'blank-subject'    | '{"subject":"   "}'
            'oversized'        | '{"subject":"' + ('x' * 5000) + '"}'
            'bad cooldown'     | '{"subject":"x","cooldown":"abc"}'
    }
}
