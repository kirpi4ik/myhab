package eu.devexpert.madhouse.domain.device


import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.common.BaseEntity

class Device extends BaseEntity {
  String code
  String name
  DeviceModel model
  DeviceType type
  String description
  String offlineScenario
  NetworkAddress networkAddress
  Set<DevicePort> ports
  Set<DeviceAccount> authAccounts
  Rack rack;
  static belongsTo = [rack: Rack, type: DeviceType]
  static hasMany = [authAccounts: DeviceAccount, ports: DevicePort]

  static mapping = {
    table '`device_controllers`'
  }
  static constraints = {
    name nullable: true
    model nullable: true
    description nullable: true
    networkAddress nullable: true
    offlineScenario nullable: true
    rack nullable: true
    type nullable: true
  }
  static embedded = ['networkAddress']
  static graphql = true
}

class NetworkAddress {
  String ip
  String gateway
  String port
  static constraints = {
    gateway nullable: true
  }
}

