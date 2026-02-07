package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity

class EventTrigger extends BaseEntity {
  Set<EventDefinition> events
  Job job

  static belongsTo = [Job]
  static hasOne = [job: Job]
  static hasMany = [events: EventDefinition]

  static constraints = {
  }
  static mapping = {
    table '`job_triggers_events`'
    events joinTable: [name: "job_triggers_event_definitions_join", key: 'trigger_id']
    version false
  }

  static graphql = true
}
