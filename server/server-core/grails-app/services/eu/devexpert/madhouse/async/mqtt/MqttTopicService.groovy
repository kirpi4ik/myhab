package eu.devexpert.madhouse.async.mqtt

import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.domain.events.TopicName
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine
import org.springframework.messaging.Message

class MqttTopicService {
    def mqttPublishGateway

    MQTTMessage message(String topicName, Message<?> message) {
        def matcher

        switch (topicName) {
            case ~/${MQTTTopic.ESP.STATUS.regex}/:
                matcher = topicName =~ MQTTTopic.ESP.STATUS.regex
                //payload = online | offline
                return new MQTTMessage(deviceType: DeviceModel.ESP8266_1, deviceCode: matcher[0][1], portStrValue: message.payload, eventType: TopicName.EVT_DEVICE_STATUS.id())
            case ~/${MQTTTopic.ESP.READ_SINGLE_VAL.regex}/:
                matcher = topicName =~ MQTTTopic.ESP.READ_SINGLE_VAL.regex
                return new MQTTMessage(deviceType: DeviceModel.ESP8266_1, deviceCode: matcher[0][1], portType: matcher[0][2], portCode: matcher[0][3], portStrValue: message.payload, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
            case ~/${MQTTTopic.MEGA.READ_SINGLE_VAL.regex}/:
                matcher = topicName =~ MQTTTopic.MEGA.READ_SINGLE_VAL.regex
                return new MQTTMessage(deviceType: DeviceModel.MEGAD_2561_RTC, deviceCode: matcher[0][1], portCode: matcher[0][2], portStrValue: new JsonSlurper().parse(message.payload['value']).value, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
        }

        return null
    }

    String topic(Device device, MQTTMessage mqttMessage) {
        if (device.model.equals(DeviceModel.MEGAD_2561_RTC)) {
            return new SimpleTemplateEngine().createTemplate(MQTTTopic.MEGA.WRITE_SINGLE_VAL.regex).make([map: mqttMessage]).toString()
        }
        if (device.model.equals(DeviceModel.ESP8266_1)) {
            return new SimpleTemplateEngine().createTemplate(MQTTTopic.ESP.WRITE_SINGLE_VAL.regex).make([map: mqttMessage])
        }
    }

    String payload(DevicePort port, def actions) {
        if (port.device.model.equals(DeviceModel.MEGAD_2561_RTC)) {
            def act = []
            if (!actions.empty) {
                actions.each {
                    if (it instanceof PortAction) {
                        act << "${port.internalRef}:${it.value}"
                    } else {
                        act << "${it}"
                    }
                }

            }
            return act.join(";")
        }
        if (port.device.model.equals(DeviceModel.ESP8266_1)) {
            return actions
        }
    }

    def publish(DevicePort p, def actions) {
        def message = new MQTTMessage(deviceCode: p.device.code, portCode: p.internalRef, portType: p.type.name().toLowerCase(), portStrValue: payload(p, actions))
        mqttPublishGateway.sendToMqtt(topic(p.device, message), message.portStrValue)
    }

    def forceRead(DevicePort p, def actions) {
        def message = new MQTTMessage(deviceCode: p.device.code, portCode: p.internalRef, portType: p.type.name().toLowerCase(), portStrValue: payload(p, actions))
        mqttPublishGateway.sendToMqtt(topic(p.device, message), message.portStrValue)
    }
}
