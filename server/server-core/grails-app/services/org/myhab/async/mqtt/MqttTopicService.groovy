package org.myhab.async.mqtt

import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.TopicName
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine
import org.springframework.messaging.Message

import static org.myhab.async.mqtt.DeviceTopic.TopicTypes.*

class MqttTopicService {
    def mqttPublishGateway

    MQTTMessage message(String topicName, Message<?> message) {
        def matcher
        try {
            switch (topicName) {
                case ~/${MQTTTopic.ESP.topic(STATUS)}/:
                    matcher = topicName =~ MQTTTopic.ESP.topic(STATUS)
                    //payload = online | offline
                    return new MQTTMessage(deviceType: DeviceModel.ESP32, deviceCode: matcher[0][1], portStrValue: message.payload, eventType: TopicName.EVT_DEVICE_STATUS.id())
                case ~/${MQTTTopic.NIBE.topic(STATUS)}/:
                    matcher = topicName =~ MQTTTopic.NIBE.topic(STATUS)
                    return new MQTTMessage(deviceType: DeviceModel.NIBE_F1145_8_EM, deviceCode: matcher[0][1], portStrValue: message.payload, eventType: TopicName.EVT_DEVICE_STATUS.id())
                case ~/${MQTTTopic.ELECTRIC_METER_DTS.topic(READ_SINGLE_VAL)}/:
                    matcher = topicName =~ MQTTTopic.ELECTRIC_METER_DTS.topic(READ_SINGLE_VAL)
                    return new MQTTMessage(deviceType: DeviceModel.ELECTRIC_METER_DTS, deviceCode: matcher[0][2], portCode: matcher[0][3], portStrValue: message.payload, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
                case ~/${MQTTTopic.ESP.topic(READ_SINGLE_VAL)}/:
                    matcher = topicName =~ MQTTTopic.ESP.topic(READ_SINGLE_VAL)
                    return new MQTTMessage(deviceType: DeviceModel.ESP32, deviceCode: matcher[0][1], portType: matcher[0][2], portCode: matcher[0][3], portStrValue: message.payload, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
                case ~/${MQTTTopic.MEGA.topic(READ_SINGLE_VAL)}/:
                    matcher = topicName =~ MQTTTopic.MEGA.topic(READ_SINGLE_VAL)
                    def payload = [:]
                    def strPayload = message.payload as String
                    try {
                        payload = new JsonSlurper().parseText(strPayload)
                    } catch (ignored) {
                        if (strPayload.contains("NA")) {
                            payload = ["value": "NA"]
                        }
                    }
                    return new MQTTMessage(deviceType: DeviceModel.MEGAD_2561_RTC, deviceCode: matcher[0][1], portCode: matcher[0][2], portStrValue: payload.value ?: payload.v, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
                case ~/${MQTTTopic.NIBE.topic(READ_SINGLE_VAL)}/:
                    matcher = topicName =~ MQTTTopic.NIBE.topic(READ_SINGLE_VAL)
                    return new MQTTMessage(deviceType: DeviceModel.NIBE_F1145_8_EM, deviceCode: matcher[0][1], portType: matcher[0][2], portCode: matcher[0][3], portStrValue: message.payload, eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id())
                case ~/${MQTTTopic.ONVIF.topic(READ_SINGLE_VAL)}/:
                    matcher = topicName =~ MQTTTopic.ONVIF.topic(READ_SINGLE_VAL)
                    return new MQTTMessage(deviceType: DeviceModel.CAM_ONVIF, deviceCode: matcher[0][1], portStrValue: message.payload, eventType: TopicName.EVT_PRESENCE.id())
            }
        } catch (mqttParsingException) {
            log.warn("Unable to parse the message[${message.payload}]", mqttParsingException)
        }
        return null
    }

    String topic(Device d, MQTTMessage mqttMessage) {
        def topic = device(d.model)
        if (topic != null) {
            new SimpleTemplateEngine().createTemplate(topic.topicByType(WRITE_SINGLE_VAL)).make([map: mqttMessage]).toString()
        } else {
            log.error("Invalid device model: ${d.model}")
        }
    }

    DeviceTopic device(model) {
        switch (model) {
            case DeviceModel.MEGAD_2561_RTC:
                return new MQTTTopic.MEGA()
            case DeviceModel.ESP32:
                return new MQTTTopic.ESP()
            case DeviceModel.NIBE_F1145_8_EM:
                return new MQTTTopic.NIBE()
            case DeviceModel.CAM_ONVIF:
                return new MQTTTopic.ONVIF()
            case DeviceModel.HUAWEI_SUN2000_12KTL_M2:
                return new MQTTTopic.INVERTER()
            case DeviceModel.ELECTRIC_METER_DTS:
                return new MQTTTopic.ELECTRIC_METER_DTS()
            case DeviceModel.TMEZON_INTERCOM:
                break
        }
    }

    String payload(DevicePort port, def actions) {
        if (port.device.model == DeviceModel.MEGAD_2561_RTC) {
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
        if (port.device.model == DeviceModel.ESP32) {
            if (actions)
                return actions.first()
            else {
                return actions
            }
        }

        if (port.device.model == DeviceModel.NIBE_F1145_8_EM) {
            return actions
        }
        return actions

    }

    def publishPortValue(Device device, DevicePort port, String value) {
        def message = new MQTTMessage(deviceCode: device.code, portCode: port.internalRef, portType: port.type.name().toLowerCase(), portStrValue: payload(port, value))
        mqttPublishGateway.sendToMqtt(topic(device, message), message.portStrValue)
    }

    def publish(DevicePort p, def actions) {
        def message = new MQTTMessage(deviceCode: p.device.code, portCode: p.internalRef, portType: p.type.name().toLowerCase(), portStrValue: payload(p, actions))
        mqttPublishGateway.sendToMqtt(topic(p.device, message), message.portStrValue)
    }

    def forceRead(DevicePort p, def actions) {
        def message = new MQTTMessage(deviceCode: p.device.code, portCode: p.internalRef, portType: p.type.name().toLowerCase(), portStrValue: payload(p, actions))
        mqttPublishGateway.sendToMqtt(topic(p.device, message), message.portStrValue)
    }

    def publishStatus(Device d, DeviceStatus status) {
        mqttPublishGateway.sendToMqtt(new SimpleTemplateEngine()
                .createTemplate(device(d.model).topicByType(STATUS_WRITE))
                .make([map: new MQTTMessage(deviceCode: d.code)]).toString(), status.name().toLowerCase())
    }
}