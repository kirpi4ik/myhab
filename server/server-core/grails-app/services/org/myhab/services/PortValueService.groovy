package org.myhab.services

import org.myhab.ConfigKey
import org.myhab.domain.Configuration
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
                    newPortValue.portId = devicePort.id
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
        if (port == null) return

        def newVal = ValueParser.parser(port).apply(event.data.p5)

        if (newVal == PortAction.OFF.name()) {
            hazelcastInstance.getMap(CacheMap.EXPIRE.name).remove(String.valueOf(port.id))
            log.debug("Cache removed for port ${port.id}")
            // Clear any pending one-shot override on this port's peripheral. Stale
            // overrides (set by switchOn but never consumed because the device went
            // OFF before confirming ON) would otherwise be mistakenly applied to the
            // next unrelated ON event.
            if (!port.peripherals.empty) {
                def offPeripheral = port.peripherals.first()
                if (offPeripheral != null) {
                    try {
                        int deleted = Configuration.where {
                            entityId == offPeripheral.id &&
                                    entityType == EntityType.PERIPHERAL &&
                                    key == ConfigKey.STATE_ON_TIMEOUT_OVERRIDE
                        }.deleteAll()
                        if (deleted > 0) {
                            log.debug("Cleared ${deleted} stale timeout override(s) for peripheral ${offPeripheral.id} on observed OFF")
                        }
                    } catch (Exception e) {
                        log.error("Failed to clear stale override for peripheral ${offPeripheral.id}: ${e.message}", e)
                    }
                }
            }
            return
        }

        if (newVal != PortAction.ON.name()) return
        if (port.peripherals.empty) return
        def peripheral = port.peripherals.first()
        if (peripheral == null) return

        // 1. One-shot override Configuration row (key.on.timeout.override) wins over
        //    the persisted key.on.timeout for this cycle. Consumed and deleted.
        def overrideConfig = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT_OVERRIDE }
        Integer overrideSec = null
        if (overrideConfig != null) {
            try {
                overrideSec = Integer.valueOf(overrideConfig.value)
            } catch (NumberFormatException ignored) {
                log.warn("Invalid ${ConfigKey.STATE_ON_TIMEOUT_OVERRIDE} value '${overrideConfig.value}' on peripheral ${peripheral.id}")
            }
            // Always delete the override row once observed, even if the value is
            // unparseable — keeps the row from sticking around forever.
            try {
                Configuration.where { id == overrideConfig.id }.deleteAll()
            } catch (Exception e) {
                log.error("Failed to remove consumed override row ${overrideConfig.id}: ${e.message}", e)
            }
        }

        Integer timeoutSec = overrideSec
        if (timeoutSec == null) {
            // 2. Fall back to the peripheral's persisted Configuration row.
            def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
            if (config != null) {
                try {
                    timeoutSec = Integer.valueOf(config.value)
                } catch (NumberFormatException ignored) {
                    log.warn("Invalid ${ConfigKey.STATE_ON_TIMEOUT} value '${config.value}' on peripheral ${peripheral.id}")
                }
            }
        }
        if (timeoutSec == null || timeoutSec <= 0) return

        def expireInMs = DateTime.now().plusSeconds(timeoutSec).toDate().time
        hazelcastInstance.getMap(CacheMap.EXPIRE.name)
                .put(String.valueOf(port.id), [expireOn: expireInMs, peripheralId: peripheral.id])
        log.debug("Cache created for port ${port.id}, expires at ${new Date(expireInMs)}, peripheral ${peripheral.id} (override=${overrideSec != null})")
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
