package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.DomainUtil
import eu.devexpert.madhouse.domain.common.BaseEntity

/**
 *
 */
class DeviceAccount extends BaseEntity {
  String uid = DomainUtil.NULL_UID
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
