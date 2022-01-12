package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

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
