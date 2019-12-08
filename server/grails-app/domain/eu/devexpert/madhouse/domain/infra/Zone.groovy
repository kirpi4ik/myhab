package eu.devexpert.madhouse.domain.infra

import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.common.BaseEntity
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Zone extends BaseEntity {
  String name
  String description
  Set<String> categories
  Zone parent
  Set<Zone> zones
  Set<DevicePeripheral> peripherals
  Set<Cable> cables
  static belongsTo = [parent: Zone]
  static hasOne = [parent: Zone]
  static hasMany = [cables: Cable, peripherals: DevicePeripheral, zones: Zone]


  static mapping = {
    table '`zones`'
    peripherals joinTable: [name: "zones_peripherals_join", key: 'zone_id']
    cables joinTable: [name: "zones_cables_join", key: 'zone_id']
    sort parent: "asc"
  }
  static constraints = {
    peripherals cascade: 'save-update'
    cables cascade: 'save-update'
    zones cascade: 'save-update'
  }
  static graphql = true
}