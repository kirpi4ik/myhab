package org.myhab.async.mqtt

import grails.testing.services.ServiceUnitTest
import org.myhab.domain.device.DeviceModel
import org.springframework.messaging.support.GenericMessage
import spock.lang.Specification

/**
 * Routing contract for the notification channel added alongside the port pipeline:
 * `myhab/<source>/notify` must resolve to evt_user_notification, and the existing
 * port/status topics must keep resolving exactly as before.
 */
class MqttTopicServiceSpec extends Specification implements ServiceUnitTest<MqttTopicService> {

    def setup() {
        // Patterns are built in @PostConstruct, which the unit-test harness doesn't run.
        service.init()
    }

    private static GenericMessage<String> msg(String payload = 'payload') {
        new GenericMessage<String>(payload)
    }

    void "a notify topic resolves to evt_user_notification carrying the source and raw payload"() {
        when:
            def result = service.message('myhab/gate_controller/notify', msg('{"subject":"Gate opened"}'))

        then:
            result.eventType == 'evt_user_notification'
            result.deviceCode == 'gate_controller'
            result.portStrValue == '{"subject":"Gate opened"}'
            result.portCode == null
    }

    void "the notify segment is matched before the status patterns"() {
        // `myhab/notify/status` would otherwise be swallowed by ESP_STATUS.
        when:
            def result = service.message('myhab/notify/notify', msg())

        then:
            result.eventType == 'evt_user_notification'
    }

    void "an ESP port state topic still resolves to the port-value event"() {
        when:
            def result = service.message('myhab/gate_controller/switch/d0/state', msg('ON'))

        then:
            result.eventType == 'evt_mqtt_port_value_changed'
            result.deviceType == DeviceModel.ESP32
            result.deviceCode == 'gate_controller'
            result.portType == 'switch'
            result.portCode == 'd0'
    }

    void "an ESP status topic still resolves to the device-status event"() {
        when:
            def result = service.message('myhab/gate_controller/status', msg('online'))

        then:
            result.eventType == 'evt_device_status'
            result.deviceCode == 'gate_controller'
    }

    void "command topic echoes are still ignored"() {
        expect:
            service.message('myhab/gate_controller/switch/d0/cmd', msg('ON')) == null
    }
}
