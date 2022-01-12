package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.Scenario
import org.myhab.domain.device.port.DevicePort

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
