package eu.devexpert.madhouse.domain.job

import eu.devexpert.madhouse.domain.common.BaseEntity

class EventDefinition extends BaseEntity {
  String name
  boolean hasSubscriber
  boolean hasPublisher

  static belongsTo = [EventTrigger]

  static mapping = {
    table '`event_definitions`'
    version false
  }
}
