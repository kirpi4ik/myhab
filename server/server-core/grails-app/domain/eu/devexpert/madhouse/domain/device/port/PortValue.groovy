package eu.devexpert.madhouse.domain.device.port


import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.job.EventData

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
