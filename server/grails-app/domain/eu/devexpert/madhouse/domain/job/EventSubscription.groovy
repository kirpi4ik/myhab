package eu.devexpert.madhouse.domain.job

import eu.devexpert.madhouse.domain.device.Scenario
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.common.BaseEntity

class EventSubscription extends BaseEntity {
  Set<EventDefinition> events
  Scenario scenario
  DevicePort pubPort

  static hasMany = [events: EventDefinition]
  static belongsTo = DevicePort
  static fetchMode = [events: 'eager', scenario: "eager"]
  static mapping = {
    table '`event_subscriptions`'
    events joinTable: [name: "event_definitions_subscriptions_join", key: 'subscription_id']
    events lazy: false
    scenario lazy: false
  }
}
