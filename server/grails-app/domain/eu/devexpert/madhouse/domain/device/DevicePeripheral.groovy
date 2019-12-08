package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.infra.Zone

class DevicePeripheral extends BaseEntity {
  String code
  String codeOld
  String name
  String model
  String description
  double maxAmp
  PeripheralCategory category
  Set<DevicePort> connectedTo
  Set<Zone> zones

  static belongsTo = [DevicePort, Zone, PeripheralCategory]
  static hasMany = [connectedTo: DevicePort, zones: Zone]
  static hasOne = [category: PeripheralCategory]

  static constraints = {
    name nullable: true
    model nullable: true
    description nullable: true
  }
  static mapping = {
    table '`device_peripherals`'
    connectedTo joinTable: [name: "device_ports_peripherals_join", key: 'peripheral_id']
    zones joinTable: [name: "zones_peripherals_join", key: 'peripheral_id']
    sort code: "asc"
  }

  static graphql = true
}
