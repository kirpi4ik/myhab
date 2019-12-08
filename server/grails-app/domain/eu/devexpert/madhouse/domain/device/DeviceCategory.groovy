package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class DeviceCategory extends BaseEntity {
  String name
  String description

  static constraints = {
  }
  static mapping = {
    table '`device_categories`'
  }
    static graphql = true
}
