package org.myhab.services

import org.myhab.ConfigKey
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.device.port.PortValue
import org.myhab.init.cache.CacheMap
import org.myhab.parser.ValueParser
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.domain.common.Event
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.springframework.transaction.annotation.Propagation

@Slf4j
@Transactional(propagation = Propagation.REQUIRED)
class PortValueService implements EventPublisher {
    def hazelcastInstance;
    def deviceService
    def configProvider

    @Subscriber("evt_async_port_value_changed")
    void updateAsyncPort(event) {
        updatePort(event)
    }

    @Subscriber('evt_mqtt_port_value_changed')
    def updatePort(event) {
        def device = Device.findByCode(event.data.p2, [lock: true])
        if (device == null && configProvider.get(Boolean.class, "admin.devices.autoimport")) {
            device = deviceService.importDevice(event.data.p1, event.data.p2)
        }

        def devicePort = device?.ports?.find {
            port -> port.internalRef == event.data.p4
        }
        if (device != null && devicePort == null && configProvider.get(Boolean.class, "admin.ports.autoimport")) {
            devicePort = deviceService.importPort(device, event.data.p3, event.data.p4)
        }
        if (devicePort != null) {
            def newVal = ValueParser.parser(devicePort).apply(event.data.p5)
            
            if (devicePort.value != newVal) {
                try {
                    PortValue newPortValue = new PortValue()
                    // Use id instead of uid (uid is deprecated)
                    newPortValue.portId = devicePort.id
                    // Set portUid for backward compatibility with database NOT NULL constraint
                    // beforeInsert will also set this, but set it here too to ensure it's never null
                    newPortValue.portUid = devicePort.id?.toString() ?: ''
                    newPortValue.value = newVal
                    newPortValue.save(failOnError: false, flush: true)

                    devicePort.setValue(newVal)
                    devicePort.save(failOnError: false, flush: true)

                    publish(TopicName.EVT_PORT_VALUE_PERSISTED.id(), new EventData().with {
                        p0 = TopicName.EVT_PORT_VALUE_PERSISTED.id()
                        p1 = EntityType.PORT.name()
                        p2 = "${devicePort.id}"
                        p3 = "${devicePort.internalRef}"
                        p4 = "${devicePort.value}"
                        p6 = "${event.data.p6}"
                        it
                    })
                } catch (Exception ex) {
                    log.error("Error saving port value: ${ex.message}", ex)
                }
                publish(TopicName.EVT_UI_UPDATE_PORT_VALUE.id(), new Event().with {
                    p0 = TopicName.EVT_PORT_VALUE_PERSISTED.id()
                    p1 = EntityType.PORT.name()
                    p2 = "${devicePort.id}"
                    p3 = "${devicePort.internalRef}"
                    p4 = "${devicePort.value}"
                    p6 = "${event.data.p6}"
                    it
                })
            }
        }
    }

    @Subscriber("evt_mqtt_port_value_changed")
    def updateExpirationTime(event) {
        log.debug("evt_mqtt_port_value_changed received: device=${event.data.p2}, ref=${event.data.p4}, value=${event.data.p5}")
        
        def port = Device.findByCode(event.data.p2, [lock: true])?.ports?.find {
            port -> port.internalRef == event.data.p4
        }
        
        if (port != null) {
            def newVal = ValueParser.parser(port).apply(event.data.p5)

            if (!port.peripherals.empty) {
                def peripheral = port.peripherals?.first()
                if (peripheral != null) {
                    def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
                    
                if (config != null && newVal == PortAction.ON.name()) {
                    def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(config.value)).toDate().time
                    hazelcastInstance.getMap(CacheMap.EXPIRE.name).put(String.valueOf(port.id), [expireOn: expireInMs, peripheralId: peripheral.id])
                    log.debug("Cache created for port ${port.id}, expires at ${new Date(expireInMs)}, peripheral ${peripheral.id}")
                } else if (newVal == PortAction.OFF.name()) {
                    hazelcastInstance.getMap(CacheMap.EXPIRE.name).remove(String.valueOf(port.id))
                    log.debug("Cache removed for port ${port.id}")
                }
                }
            }
        }
    }

    @Subscriber("evt_cfg_value_changed")
    def updateExpirationTimeOnCfg(event) {
        if (event.data.p2 == EntityType.PERIPHERAL.name() && event.data.p4 == ConfigKey.STATE_ON_TIMEOUT) {
            def peripheral = DevicePeripheral.findById(Long.valueOf(event.data.p3))
            if (peripheral != null && peripheral.connectedTo) {
                def firstPort = peripheral.connectedTo.first()
                
            if (firstPort.value == PortAction.ON.name()) {
                def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(event.data.p5)).toDate().time
                hazelcastInstance.getMap(CacheMap.EXPIRE.name).put(String.valueOf(firstPort.id), [expireOn: expireInMs, peripheralId: peripheral.id])
                log.debug("Cache updated for port ${firstPort.id} due to config change, expires at ${new Date(expireInMs)}, peripheral ${peripheral.id}")
            }
            }
        }
    }

}
