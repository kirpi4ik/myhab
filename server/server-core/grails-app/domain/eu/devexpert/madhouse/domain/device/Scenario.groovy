package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.port.DevicePort

class Scenario extends BaseEntity {
  String name
  String body
  Set<DevicePort> ports

  static hasMany = [ports: DevicePort]
  static belongsTo = DevicePort

  static constraints = {
    name nullable: true
  }

  static mapping = {
    table '`scenarios`'
    ports joinTable: [name: "device_ports_scenarios_join", key: 'scenario_id']
  }
  static graphql = true
}
