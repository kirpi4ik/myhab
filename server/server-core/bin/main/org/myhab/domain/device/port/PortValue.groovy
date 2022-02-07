package org.myhab.domain.device.port


import org.myhab.domain.common.BaseEntity
import org.myhab.domain.job.EventData

/**
 * Avoid scanning this data, search only by keys
 */
class PortValue extends BaseEntity {
  String portUid
  String value
    EventData event

  static constraints = {
    event nullable: true
  }
  static mapping = {
    table '`port_values`'
    version false
  }
  static graphql = true
}
