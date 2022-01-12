package org.myhab.services


import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import grails.gorm.transactions.Transactional
import org.myhab.async.mqtt.MqttTopicService

@Transactional
class HeatService {
    MqttTopicService mqttTopicService

    def heatOn(DevicePeripheral peripheral) {
        peripheral.connectedTo.each {
            mqttTopicService.publish(it, [PortAction.ON])
        }
    }

    def heatOff(DevicePeripheral peripheral) {
        peripheral.connectedTo.each {
            mqttTopicService.publish(it, [PortAction.OFF])
        }
    }
}
