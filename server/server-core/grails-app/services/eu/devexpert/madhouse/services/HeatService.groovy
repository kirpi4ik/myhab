package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.async.mqtt.MqttTopicService
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.port.PortAction
import grails.gorm.transactions.Transactional

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
