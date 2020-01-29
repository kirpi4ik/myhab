package eu.devexpert.madhouse.domain.infra

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Layer extends BaseEntity {
  String name
  String description
  Set<DevicePeripheral> peripherals;
  static hasMany = [peripherals: DevicePeripheral]

  static mapping = {
    table '`layers`'
    peripherals joinTable: [name: "layers_peripherals_join", key: 'layer_id']
    sort name: "desc"
  }
  static constraints = {
    peripherals cascade: 'save-update'
  }
  static graphql = true
}
