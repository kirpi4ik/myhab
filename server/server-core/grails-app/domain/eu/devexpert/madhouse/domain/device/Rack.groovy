package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.infra.Zone

class Rack extends BaseEntity {
  String name
  String description
  Zone zone;
  Set<Device> devices
  Set<Cable> cables
  static belongsTo = [zone: Zone]
  static hasMany = [devices: Device, cables: Cable]

  static constraints = {
    zone nullable: true
    name nullable: true
    description nullable: true
  }
  static mapping = {
    table '`racks`'

  }
  static graphql = true
}