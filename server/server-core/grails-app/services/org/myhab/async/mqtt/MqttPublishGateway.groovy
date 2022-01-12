package org.myhab.async.mqtt

import org.springframework.integration.annotation.MessagingGateway
import org.springframework.integration.mqtt.support.MqttHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
interface MqttPublishGateway {
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, @Payload String payload);
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, int qos, @Payload String payload);
}
