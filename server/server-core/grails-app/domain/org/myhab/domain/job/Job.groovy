package org.myhab.domain.job

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.Scenario

@Slf4j
class Job extends BaseEntity {
    String name
    String description
    List<CronTrigger> cronTriggers
    List<EventTrigger> eventTriggers
    Scenario scenario
    JobState state
    Set<JobTag> tags

    // Store previous state to detect changes
    transient JobState previousState

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
        scenario nullable: true
        description nullable: true
        state nullable: true
        cronTriggers nullable: true
        eventTriggers nullable: true
    }

    static graphql = true

    void beforeUpdate() {
        // Call parent to update timestamps
        super.beforeUpdate()
        
        // Store the previous state before update
        if (id && isDirty('state')) {
            // Get the original value from the database
            def originalJob = Job.get(id)
            if (originalJob) {
                previousState = originalJob.state
            }
        }
    }

    void afterUpdate() {
        // Only process if state actually changed
        if (previousState != state) {
            handleStateChange()
        }
    }

    void afterInsert() {
        // Handle new jobs
        if (state) {
            handleStateChange()
        }
    }

    private void handleStateChange() {
        try {
            def schedulerService = Holders.grailsApplication.mainContext.getBean('schedulerService')
            
            if (state == JobState.ACTIVE) {
                // Job is now ACTIVE - schedule it in Quartz
                log.info("Job ${id} state changed to ACTIVE, scheduling in Quartz")
                schedulerService.scheduleJobInQuartz(id)
            } else {
                // Job is not ACTIVE - unschedule it from Quartz
                log.info("Job ${id} state changed to ${state}, unscheduling from Quartz")
                schedulerService.unscheduleJobFromQuartz(id)
            }
        } catch (Exception e) {
            log.error("Error handling state change for job ${id}: ${e.message}", e)
            // Don't throw exception to avoid breaking the save operation
        }
    }
}
