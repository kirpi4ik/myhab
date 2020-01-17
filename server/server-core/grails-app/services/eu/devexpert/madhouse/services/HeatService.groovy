package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.device.DevicePeripheral
import grails.gorm.transactions.Transactional

@Transactional
class HeatService {
  EspDeviceService espDeviceService

  def heatOn(DevicePeripheral peripheral) {
    peripheral.connectedTo.each {
      espDeviceService.setValue(it, "ON")
    }
  }

  def heatOff(DevicePeripheral peripheral) {
    peripheral.connectedTo.each {
      espDeviceService.setValue(it, "OFF")
    }
  }
}
