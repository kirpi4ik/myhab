package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.Scenario

class Job extends BaseEntity {
    String name
    String description
    List<CronTrigger> cronTriggers
    List<EventTrigger> eventTriggers
    Scenario scenario
    JobState state
    Set<JobTag> tags


    static fetchMode = [tags: "eager"]
    static hasMany = [cronTriggers: CronTrigger, eventTriggers: EventTrigger, tags: JobTag]

    static mapping = {
        table '`jobs`'
        tags joinTable: [name: "jobs_tags_join", key: 'job_id']
        cronTriggers cascade: "all-delete-orphan"
        eventTriggers cascade: "all-delete-orphan"
    }

    static constraints = {
        tags nullable: true
    }
}
