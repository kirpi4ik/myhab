package org.myhab.domain.job

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.Scenario
import org.myhab.domain.UserFavJobJoin
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher

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

    static graphql = GraphQLMapping.lazy {
        /**
         * Custom delete mutation to properly handle cascade deletion
         * This removes the job from users' favorites before deleting
         */
        mutation('jobDeleteCascade', 'JobDeleteResult') {
            argument('id', Long)
            returns {
                field('error', String)
                field('success', Boolean)
            }
            dataFetcher(new DeleteEntityDataFetcher(Job.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    Long jobId = environment.getArgument("id") as Long
                    
                    try {
                        def result = null
                        Job.withTransaction { status ->
                            try {
                                Job job = Job.findById(jobId)
                                
                                if (!job) {
                                    log.warn("Job not found with id: ${jobId}")
                                    result = [success: false, error: "Job not found with id: ${jobId}"]
                                    return
                                }
                                
                                log.info("Deleting job ${jobId} (${job.name})")
                                
                                // 1. Unschedule from Quartz if active
                                if (job.state == JobState.ACTIVE) {
                                    try {
                                        def schedulerService = Holders.grailsApplication.mainContext.getBean('schedulerService')
                                        schedulerService.unscheduleJobFromQuartz(jobId)
                                        log.debug("Unscheduled job ${jobId} from Quartz")
                                    } catch (Exception e) {
                                        log.warn("Could not unschedule job ${jobId} from Quartz: ${e.message}")
                                    }
                                }
                                
                                // 2. Remove job from all users' favorite jobs using join table
                                int removedCount = UserFavJobJoin.removeAllForJob(jobId)
                                log.debug("Removed job ${jobId} from ${removedCount} user(s) favorite lists")
                                
                                // 3. Delete the job (cascade will handle cronTriggers and eventTriggers)
                                job.delete(flush: true)
                                
                                log.info("Successfully deleted job ${jobId}")
                                result = [success: true, error: null]
                            } catch (Exception txEx) {
                                status.setRollbackOnly()
                                throw txEx
                            }
                        }
                        return result
                    } catch (Exception e) {
                        log.error("Failed to delete job ${jobId}: ${e.message}", e)
                        return [success: false, error: "Failed to delete job: ${e.message}"]
                    }
                }
            })
        }
    }

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
        try {
            // Check if state changed
            boolean stateChanged = previousState != state
            
            // Handle state change or reschedule if ACTIVE job was updated
            if (stateChanged) {
                handleStateChange()
            } else if (state == JobState.ACTIVE) {
                // Job is ACTIVE and was updated (e.g., name, triggers, etc.)
                // Reschedule to apply changes
                log.info("Job ${id} updated while ACTIVE, rescheduling to apply changes")
                rescheduleInQuartz()
            }
        } catch (Exception e) {
            log.error("Error handling job update for job ${id}: ${e.message}", e)
            // Don't throw exception to avoid breaking the save operation
        }
    }

    void afterInsert() {
        // Handle new jobs
        if (state) {
            handleStateChange()
        }
    }

    /**
     * Handle job state changes (ACTIVE, DISABLED, DRAFT)
     */
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

    /**
     * Reschedule an ACTIVE job in Quartz
     * 
     * <p>This is used when job details or cron triggers are updated while the job is ACTIVE.
     * It unschedules all existing triggers and reschedules them with the updated configuration.</p>
     */
    private void rescheduleInQuartz() {
        try {
            def schedulerService = Holders.grailsApplication.mainContext.getBean('schedulerService')
            schedulerService.unscheduleJobFromQuartz(id)
            schedulerService.scheduleJobInQuartz(id)
            log.info("Job ${id} rescheduled successfully in Quartz")
        } catch (Exception e) {
            log.error("Error rescheduling job ${id} in Quartz: ${e.message}", e)
            // Don't throw exception to avoid breaking the save operation
        }
    }
}
