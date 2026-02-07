package org.myhab.domain.device

import org.myhab.domain.DomainUtil
import org.myhab.domain.common.BaseEntity

/**
 *
 */
class DeviceAccount extends BaseEntity {
  String username
  String password
  boolean isDefault
  Device device

  static constraints = {
    username nullable: true
    password nullable: true
    isDefault nullable: true
  }

  static belongsTo = Device
  static mapping = {
    table '`device_accounts`'
    password column: '`password`'
  }
  static graphql = true
}
