package org.myhab.domain.job

import groovy.util.logging.Slf4j

/**
 * Domain class to store Quartz job execution history.
 * Records execution details including start time, end time, duration, and result status.
 *
 * <p>This entity is populated by the {@link org.myhab.jobs.listener.JobHistoryListener}
 * which intercepts Quartz job execution events.</p>
 */
@Slf4j
class JobExecutionHistory {

    /**
     * Reference to the Job domain object (for dynamic jobs).
     * Can be null for static/system jobs.
     */
    Job job

    /**
     * Quartz job name (e.g., "job_123" for dynamic jobs, "nibeInfoSyncJobDetail" for static jobs)
     */
    String jobName

    /**
     * Quartz job group (e.g., "job_group_123" for dynamic jobs, "STATIC_JOBS" for static jobs)
     */
    String jobGroup

    /**
     * Quartz trigger name that fired this execution
     */
    String triggerName

    /**
     * Quartz trigger group
     */
    String triggerGroup

    /**
     * Unique fire instance ID from Quartz
     */
    String fireInstanceId

    /**
     * Timestamp when the job started executing
     */
    Date startTime

    /**
     * Timestamp when the job finished executing
     */
    Date endTime

    /**
     * Execution duration in milliseconds
     */
    Long durationMs

    /**
     * Execution status: STARTED, COMPLETED, FAILED, VETOED
     */
    JobExecutionStatus status

    /**
     * Error message if the job failed
     */
    String errorMessage

    /**
     * Exception class name if the job failed
     */
    String exceptionClass

    /**
     * Whether the job was recovering from a failed instance
     */
    Boolean recovering = false

    /**
     * Number of times the trigger has fired (refire count)
     */
    Integer refireCount = 0

    /**
     * Scheduled fire time (when the job was supposed to fire)
     */
    Date scheduledFireTime

    /**
     * Actual fire time (when the job actually fired)
     */
    Date actualFireTime

    /**
     * Timestamp when this record was created
     */
    Date tsCreated

    /**
     * Timestamp when this record was last updated
     */
    Date tsUpdated

    static belongsTo = [job: Job]

    static mapping = {
        table 'job_execution_history'
        job column: 'job_id', nullable: true
        errorMessage type: 'text'
        version false
    }

    static constraints = {
        job nullable: true
        jobName maxSize: 255
        jobGroup maxSize: 255
        triggerName nullable: true, maxSize: 255
        triggerGroup nullable: true, maxSize: 255
        fireInstanceId nullable: true, maxSize: 255
        startTime nullable: true
        endTime nullable: true
        durationMs nullable: true
        status nullable: false
        errorMessage nullable: true
        exceptionClass nullable: true, maxSize: 500
        recovering nullable: true
        refireCount nullable: true
        scheduledFireTime nullable: true
        actualFireTime nullable: true
        tsCreated nullable: true
        tsUpdated nullable: true
    }

    static graphql = true

    def beforeInsert() {
        tsCreated = tsCreated ?: new Date()
        tsUpdated = new Date()
    }

    def beforeUpdate() {
        tsUpdated = new Date()
    }

    /**
     * Calculate duration if start and end times are set
     */
    void calculateDuration() {
        if (startTime && endTime) {
            durationMs = endTime.time - startTime.time
        }
    }

    @Override
    String toString() {
        return "JobExecutionHistory[id=${id}, jobName=${jobName}, status=${status}, startTime=${startTime}]"
    }
}
