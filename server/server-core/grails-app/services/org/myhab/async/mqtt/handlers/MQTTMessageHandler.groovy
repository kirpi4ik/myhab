package org.myhab.async.mqtt.handlers


import org.myhab.domain.job.EventData
import grails.events.EventPublisher
import groovy.util.logging.Slf4j
import org.myhab.async.mqtt.MQTTMessage
import org.myhab.async.mqtt.MqttTopicService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Component

@Slf4j
@Component
class MQTTMessageHandler implements MessageHandler, EventPublisher {
    @Autowired
    MqttTopicService mqttTopicService
    def espStateRegex = "madhouse/(\\w+|_+)/status" //online | offline

    @Override
    void handleMessage(Message<?> message) throws MessagingException {
        try {
            def topicName = message.headers.get("mqtt_receivedTopic") as String
            MQTTMessage msg = mqttTopicService.message(topicName, message)
            if (msg != null) {
                publish(msg.eventType, new EventData().with {
                    p0 = msg.eventType
                    p1 = "${msg.deviceType}"
                    p2 = "${msg.deviceCode}" // device internal code
                    p3 = "${msg.portType}" // port type (optional)
                    p4 = "${msg.portCode}" // port internal code
                    p5 = "${msg.portStrValue}" // port internal code
                    p6 = "mqtt"
                    it
                })
//                log.info("device code = ${msg.deviceCode}, port type = ${msg.portType}, port code = ${msg.portCode}, value = ${msg.portStrValue}")
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
