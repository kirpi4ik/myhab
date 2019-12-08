package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class EventLog extends BaseEntity {
  DevicePeripheral peripheral
  String payload


  static mapping = {
    table '`device_event_logs`'
    version false
  }
  static graphql = true
}
