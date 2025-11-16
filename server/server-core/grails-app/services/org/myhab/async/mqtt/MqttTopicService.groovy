package org.myhab.async.mqtt

import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.TopicName
import groovy.json.JsonSlurper
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.springframework.messaging.Message

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

import static org.myhab.async.mqtt.DeviceTopic.TopicTypes.*

class MqttTopicService {
    def mqttPublishGateway
    
    // Reusable template engine
    private final SimpleTemplateEngine templateEngine = new SimpleTemplateEngine()
    
    // Pre-compiled regex patterns
    private static final Map<String, Pattern> TOPIC_PATTERNS = [:]
    
    // Cached device topics
    private static final Map<DeviceModel, DeviceTopic> DEVICE_TOPIC_CACHE = new ConcurrentHashMap<>()
    
    // Cached templates
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>()
    
    @PostConstruct
    void init() {
        // Automatically discover and compile all topic patterns
        initializeTopicPatterns()
        
        // Automatically populate device topic cache
        initializeDeviceTopicCache()
    }
    
    private void initializeTopicPatterns() {
        // List of all DeviceTopic implementations in MQTTTopic
        def deviceTopics = [
            [name: 'ESP', instance: new MQTTTopic.ESP()],
            [name: 'NIBE', instance: new MQTTTopic.NIBE()],
            [name: 'ELECTRIC_METER', instance: new MQTTTopic.ELECTRIC_METER_DTS()],
            [name: 'INVERTER', instance: new MQTTTopic.INVERTER()],
            [name: 'MEGA', instance: new MQTTTopic.MEGA()],
            [name: 'COMMON', instance: new MQTTTopic.COMMON()],
            [name: 'ONVIF', instance: new MQTTTopic.ONVIF()],
            [name: 'METEO_STATION', instance: new MQTTTopic.METEO_STATION()]
        ]
        
        // READ topic types (incoming messages from devices)
        def readTopicTypes = [
            [type: STATUS, suffix: 'STATUS'],
            [type: READ_SINGLE_VAL, suffix: 'READ'],
            [type: STAT_IP, suffix: 'STAT_IP'],
            [type: STAT_PORT, suffix: 'STAT_PORT']
        ]
        
        // Compile patterns for all combinations
        deviceTopics.each { device ->
            readTopicTypes.each { topicType ->
                def key = "${device.name}_${topicType.suffix}"
                def pattern = device.instance.topicByType(topicType.type)
                compilePattern(key, pattern)
            }
        }
        
        log.info("Initialized ${TOPIC_PATTERNS.size()} topic patterns")
    }
    
    private void initializeDeviceTopicCache() {
        // Map device models to their topic implementations
        def deviceMappings = [
            [model: DeviceModel.MEGAD_2561_RTC, topic: new MQTTTopic.MEGA()],
            [model: DeviceModel.ESP32, topic: new MQTTTopic.ESP()],
            [model: DeviceModel.NIBE_F1145_8_EM, topic: new MQTTTopic.NIBE()],
            [model: DeviceModel.CAM_ONVIF, topic: new MQTTTopic.ONVIF()],
            [model: DeviceModel.HUAWEI_SUN2000_12KTL_M2, topic: new MQTTTopic.INVERTER()],
            [model: DeviceModel.ELECTRIC_METER_DTS, topic: new MQTTTopic.ELECTRIC_METER_DTS()],
            [model: DeviceModel.OPEN_METEO_API, topic: new MQTTTopic.METEO_STATION()]
        ]
        
        deviceMappings.each { mapping ->
            DEVICE_TOPIC_CACHE.put(mapping.model, mapping.topic)
        }
        
        log.info("Initialized ${DEVICE_TOPIC_CACHE.size()} device topic mappings")
    }
    
    private void compilePattern(String key, String topicPattern) {
        if (topicPattern) {
            try {
                TOPIC_PATTERNS.put(key, ~/${topicPattern}/)
            } catch (Exception e) {
                log.warn("Failed to compile pattern for ${key}: ${topicPattern}", e)
            }
        } else {
            log.debug("No topic pattern defined for ${key}")
        }
    }

    MQTTMessage message(String topicName, Message<?> message) {
        if (!topicName || !message) {
            log.warn("Invalid input: topicName=${topicName}, message=${message}")
            return null
        }
        
        // Ignore outgoing command topics (write topics that get echoed back)
        if (isCommandTopic(topicName)) {
            log.trace("Ignoring command topic echo: ${topicName}")
            return null
        }
        
        try {
            def matcher
            
            // ========== STATUS PATTERNS ==========
            if (topicName ==~ TOPIC_PATTERNS.ESP_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.ESP_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.ESP32,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.NIBE_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.NIBE_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.NIBE_F1145_8_EM,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.ELECTRIC_METER_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.ELECTRIC_METER_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.ELECTRIC_METER_DTS,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.INVERTER_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.INVERTER_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.HUAWEI_SUN2000_12KTL_M2,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.MEGA_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.MEGA_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.MEGAD_2561_RTC,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.COMMON_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.COMMON_STATUS
                return new MQTTMessage(
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.METEO_STATION_STATUS) {
                matcher = topicName =~ TOPIC_PATTERNS.METEO_STATION_STATUS
                return new MQTTMessage(
                    deviceType: DeviceModel.OPEN_METEO_API,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_DEVICE_STATUS.id()
                )
            }
            
            // ========== READ_SINGLE_VAL PATTERNS ==========
            if (topicName ==~ TOPIC_PATTERNS.ESP_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.ESP_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.ESP32,
                    deviceCode: matcher[0][1],
                    portType: matcher[0][2],
                    portCode: matcher[0][3],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.NIBE_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.NIBE_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.NIBE_F1145_8_EM,
                    deviceCode: matcher[0][1],
                    portType: matcher[0][2],
                    portCode: matcher[0][3],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.ELECTRIC_METER_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.ELECTRIC_METER_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.ELECTRIC_METER_DTS,
                    deviceCode: matcher[0][1],
                    portCode: matcher[0][3],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.INVERTER_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.INVERTER_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.HUAWEI_SUN2000_12KTL_M2,
                    deviceCode: matcher[0][1],
                    portCode: matcher[0][3],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.MEGA_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.MEGA_READ
                def payload = parseJsonPayload(message.payload as String)
                return new MQTTMessage(
                    deviceType: DeviceModel.MEGAD_2561_RTC,
                    deviceCode: matcher[0][1],
                    portCode: matcher[0][2],
                    portStrValue: payload.value ?: payload.v,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.COMMON_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.COMMON_READ
                def payload = parseJsonPayload(message.payload as String)
                return new MQTTMessage(
                    deviceCode: matcher[0][1],
                    portCode: matcher[0][2],
                    portStrValue: payload.value ?: payload.v,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.ONVIF_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.ONVIF_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.CAM_ONVIF,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_PRESENCE.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.METEO_STATION_READ) {
                matcher = topicName =~ TOPIC_PATTERNS.METEO_STATION_READ
                return new MQTTMessage(
                    deviceType: DeviceModel.OPEN_METEO_API,
                    deviceCode: matcher[0][1],
                    portCode: matcher[0][2],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            // ========== STAT_IP PATTERNS ==========
            if (topicName ==~ TOPIC_PATTERNS.ESP_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.ESP_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.ESP32,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.NIBE_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.NIBE_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.NIBE_F1145_8_EM,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.ELECTRIC_METER_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.ELECTRIC_METER_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.ELECTRIC_METER_DTS,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.INVERTER_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.INVERTER_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.HUAWEI_SUN2000_12KTL_M2,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.MEGA_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.MEGA_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.MEGAD_2561_RTC,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.COMMON_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.COMMON_STAT_IP
                return new MQTTMessage(
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.METEO_STATION_STAT_IP) {
                matcher = topicName =~ TOPIC_PATTERNS.METEO_STATION_STAT_IP
                return new MQTTMessage(
                    deviceType: DeviceModel.OPEN_METEO_API,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            // ========== STAT_PORT PATTERNS ==========
            if (topicName ==~ TOPIC_PATTERNS.ESP_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.ESP_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.ESP32,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.NIBE_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.NIBE_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.NIBE_F1145_8_EM,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.ELECTRIC_METER_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.ELECTRIC_METER_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.ELECTRIC_METER_DTS,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.INVERTER_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.INVERTER_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.HUAWEI_SUN2000_12KTL_M2,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.MEGA_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.MEGA_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.MEGAD_2561_RTC,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.COMMON_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.COMMON_STAT_PORT
                return new MQTTMessage(
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            if (topicName ==~ TOPIC_PATTERNS.METEO_STATION_STAT_PORT) {
                matcher = topicName =~ TOPIC_PATTERNS.METEO_STATION_STAT_PORT
                return new MQTTMessage(
                    deviceType: DeviceModel.OPEN_METEO_API,
                    deviceCode: matcher[0][1],
                    portStrValue: message.payload,
                    eventType: TopicName.EVT_MQTT_PORT_VALUE_CHANGED.id()
                )
            }
            
            log.debug("No matching read pattern for topic: ${topicName} (this may be normal for unsupported device types)")
            return null
            
        } catch (Exception e) {
            log.error("Error parsing MQTT message from topic ${topicName}", e)
            return null
        }
    }
    
    private boolean isCommandTopic(String topicName) {
        // Check if this is a command/write topic that should be ignored
        // These are outgoing topics that may get echoed back by the broker
        return topicName ==~ /.*\/cmd$/ ||           // MEGA: mp50/cmd
               topicName ==~ /.*\/cmd\/.*/ ||        // Generic command topics
               topicName ==~ /.*\/set$/ ||           // Common set pattern
               topicName ==~ /.*\/set\/.*/ ||        // Generic set topics
               topicName ==~ /.*\/address_ip\/state$/ ||  // Status topics we write to
               topicName ==~ /.*\/address_port\/state$/   // Status topics we write to
    }
    
    private Map parseJsonPayload(String strPayload) {
        try {
            return new JsonSlurper().parseText(strPayload)
        } catch (Exception e) {
            if (strPayload?.contains("NA")) {
                return [value: "NA"]
            }
            log.warn("Failed to parse JSON payload: ${strPayload}")
            return [:]
        }
    }

    String topic(Device d, MQTTMessage mqttMessage) {
        if (!d?.model) {
            log.warn("Cannot generate topic: device or model is null")
            return null
        }
        
        def deviceTopic = getDeviceTopic(d.model)
        if (!deviceTopic) {
            log.error("Unsupported device model: ${d.model}")
            return null
        }
        
        try {
            def topicTemplate = deviceTopic.topicByType(WRITE_SINGLE_VAL)
            def template = getOrCreateTemplate(topicTemplate)
            return template.make([map: mqttMessage]).toString()
        } catch (Exception e) {
            log.error("Error generating topic for device ${d.code}", e)
            return null
        }
    }
    
    DeviceTopic getDeviceTopic(DeviceModel model) {
        if (!model) {
            return null
        }
        return DEVICE_TOPIC_CACHE.get(model)
    }
    
    private Template getOrCreateTemplate(String templateStr) {
        return templateCache.computeIfAbsent(templateStr) {
            templateEngine.createTemplate(it)
        }
    }

    String payload(DevicePort port, def actions) {
        if (!port?.device?.model) {
            log.warn("Cannot generate payload: port or device model is null")
            return null
        }
        
        switch (port.device.model) {
            case DeviceModel.MEGAD_2561_RTC:
                return buildMegaPayload(port, actions)
                
            case DeviceModel.ESP32:
                return actions ? actions.first() : actions
                
            case DeviceModel.OPEN_METEO_API:
                // Virtual device - values published directly, no command payload needed
                return actions
                
            case DeviceModel.NIBE_F1145_8_EM:
            default:
                return actions
        }
    }
    
    private String buildMegaPayload(DevicePort port, def actions) {
        if (!actions || actions.empty) {
            return ""
        }
        
        def act = actions.collect { action ->
            action instanceof PortAction ? 
                "${port.internalRef}:${action.value}" : 
                "${action}"
        }
        
        return act.join(";")
    }

    void publishPortValue(Device device, DevicePort port, String value) {
        if (!validatePublishInputs(device, port)) {
            return
        }
        
        publishToPort(port, value)
    }

    void publish(DevicePort port, def actions) {
        if (!validatePort(port)) {
            return
        }
        
        publishToPort(port, actions)
    }

    void forceRead(DevicePort port, def actions) {
        // forceRead is functionally identical to publish
        publish(port, actions)
    }
    
    private void publishToPort(DevicePort port, def actions) {
        try {
            def message = new MQTTMessage(
                deviceCode: port.device.code,
                portCode: port.internalRef,
                portType: port.type.name().toLowerCase(),
                portStrValue: payload(port, actions)
            )
            
            def topicStr = topic(port.device, message)
            if (topicStr) {
                mqttPublishGateway.sendToMqtt(topicStr, message.portStrValue)
            } else {
                log.warn("Cannot publish: topic generation failed for port ${port.internalRef}")
            }
        } catch (Exception e) {
            log.error("Error publishing to port ${port.internalRef}", e)
        }
    }
    
    private boolean validatePublishInputs(Device device, DevicePort port) {
        if (!device) {
            log.warn("Cannot publish: device is null")
            return false
        }
        if (!port) {
            log.warn("Cannot publish: port is null for device ${device.code}")
            return false
        }
        return true
    }
    
    private boolean validatePort(DevicePort port) {
        if (!port) {
            log.warn("Cannot publish: port is null")
            return false
        }
        if (!port.device) {
            log.warn("Cannot publish: device is null for port ${port.internalRef}")
            return false
        }
        return true
    }

    void publishStatus(Device device, DeviceStatus status) {
        if (!device?.model) {
            log.warn("Cannot publish status: device or model is null (device code: ${device?.code})")
            return
        }
        
        if (!status) {
            log.warn("Cannot publish status: status is null for device ${device.code}")
            return
        }
        
        def deviceTopic = getDeviceTopic(device.model)
        if (!deviceTopic) {
            log.warn("Cannot publish status: unsupported device model ${device.model} for device ${device.code}")
            return
        }
        
        try {
            def topicTemplate = deviceTopic.topicByType(STATUS_WRITE)
            def template = getOrCreateTemplate(topicTemplate)
            def topicStr = template.make([map: new MQTTMessage(deviceCode: device.code)]).toString()
            
            mqttPublishGateway.sendToMqtt(topicStr, status.name().toLowerCase())
        } catch (Exception e) {
            log.error("Error publishing status for device ${device.code}", e)
        }
    }
}