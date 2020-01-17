package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class PeripheralCategory extends BaseEntity {

  String name
  Set<DevicePeripheral> peripherals;
  Set<Cable> cables;
  static hasMany = [peripherals: DevicePeripheral, cables: Cable]
  static mapping = {
    table '`device_peripherals_categories`'
  }
  static graphql = true
}
