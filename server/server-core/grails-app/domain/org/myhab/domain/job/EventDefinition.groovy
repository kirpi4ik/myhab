package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity

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
