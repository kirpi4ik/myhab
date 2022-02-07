package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.port.DevicePort

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
