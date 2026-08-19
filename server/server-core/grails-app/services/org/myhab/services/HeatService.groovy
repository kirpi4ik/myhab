package org.myhab.services


import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.async.mqtt.MqttTopicService

@Slf4j
@Transactional
class HeatService {
    MqttTopicService mqttTopicService

    def heatOn(DevicePeripheral peripheral) {
        peripheral.connectedTo.each {
            try {
                mqttTopicService.publish(it, [PortAction.ON])
            } catch (Exception e) {
                log.error("heatOn publish failed for port ${it.id}: ${e.message}")
            }
        }
    }

    def heatOff(DevicePeripheral peripheral) {
        peripheral.connectedTo.each {
            try {
                mqttTopicService.publish(it, [PortAction.OFF])
            } catch (Exception e) {
                log.error("heatOff publish failed for port ${it.id}: ${e.message}")
            }
        }
    }
}
