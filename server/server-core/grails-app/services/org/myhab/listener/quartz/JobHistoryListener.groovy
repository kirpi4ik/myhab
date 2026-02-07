package org.myhab.listener.quartz

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.domain.job.Job
import org.myhab.domain.job.JobExecutionHistory
import org.myhab.domain.job.JobExecutionStatus
import org.myhab.jobs.DSLJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.JobListener

/**
 * Quartz JobListener that records job execution history.
 *
 * <p>This listener intercepts job execution events and creates records in the
 * {@link JobExecutionHistory} table. It tracks:</p>
 * <ul>
 *   <li>Job start time</li>
 *   <li>Job end time and duration</li>
 *   <li>Execution status (COMPLETED, FAILED, VETOED)</li>
 *   <li>Error details if the job failed</li>
 * </ul>
 *
 * <p>Inspired by Quartz's LoggingJobHistoryPlugin but persists to database.</p>
 */
@Slf4j
class JobHistoryListener implements JobListener {

    private static final String LISTENER_NAME = "JobHistoryListener"
    
    /**
     * ThreadLocal to store the history record ID between jobToBeExecuted and jobWasExecuted
     */
    private static final ThreadLocal<Long> currentHistoryId = new ThreadLocal<>()

    @Override
    String getName() {
        return LISTENER_NAME
    }

    /**
     * Called by the Scheduler when a Job is about to be executed.
     * Creates a new JobExecutionHistory record with status STARTED.
     */
    @Override
    void jobToBeExecuted(JobExecutionContext context) {
        try {
            def jobDetail = context.jobDetail
            def trigger = context.trigger
            
            log.debug("Job [${jobDetail.key}] about to be executed by trigger [${trigger?.key}]")
            
            // Create history record asynchronously to not block job execution
            Long historyId = createHistoryRecord(context, JobExecutionStatus.STARTED)
            currentHistoryId.set(historyId)
            
        } catch (Exception e) {
            log.error("Error recording job execution start for [${context?.jobDetail?.key}]: ${e.message}", e)
            // Don't throw - we don't want to prevent job execution
        }
    }

    /**
     * Called by the Scheduler when a Job was about to be executed but a TriggerListener vetoed it.
     * Updates the JobExecutionHistory record with status VETOED.
     */
    @Override
    void jobExecutionVetoed(JobExecutionContext context) {
        try {
            def jobDetail = context.jobDetail
            log.info("Job [${jobDetail.key}] execution was vetoed")
            
            Long historyId = currentHistoryId.get()
            if (historyId) {
                updateHistoryRecord(historyId, JobExecutionStatus.VETOED, null)
                currentHistoryId.remove()
            } else {
                // Create a vetoed record if we don't have one
                createHistoryRecord(context, JobExecutionStatus.VETOED)
            }
            
        } catch (Exception e) {
            log.error("Error recording job veto for [${context?.jobDetail?.key}]: ${e.message}", e)
        }
    }

    /**
     * Called by the Scheduler after a Job has been executed.
     * Updates the JobExecutionHistory record with completion status and duration.
     */
    @Override
    void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            def jobDetail = context.jobDetail
            def runTime = context.jobRunTime
            
            if (jobException != null) {
                log.warn("Job [${jobDetail.key}] failed after ${runTime}ms: ${jobException.message}")
            } else {
                log.debug("Job [${jobDetail.key}] completed successfully in ${runTime}ms")
            }
            
            Long historyId = currentHistoryId.get()
            if (historyId) {
                JobExecutionStatus status = jobException != null ? JobExecutionStatus.FAILED : JobExecutionStatus.COMPLETED
                updateHistoryRecord(historyId, status, jobException)
                currentHistoryId.remove()
            } else {
                // Create a completed record if we don't have one (shouldn't happen normally)
                JobExecutionStatus status = jobException != null ? JobExecutionStatus.FAILED : JobExecutionStatus.COMPLETED
                Long newHistoryId = createHistoryRecord(context, status)
                if (newHistoryId && jobException != null) {
                    updateHistoryRecordWithException(newHistoryId, jobException)
                }
            }
            
        } catch (Exception e) {
            log.error("Error recording job execution completion for [${context?.jobDetail?.key}]: ${e.message}", e)
        }
    }

    /**
     * Create a new JobExecutionHistory record
     */
    @Transactional
    private Long createHistoryRecord(JobExecutionContext context, JobExecutionStatus status) {
        try {
            def jobDetail = context.jobDetail
            def trigger = context.trigger
            def mergedJobDataMap = context.mergedJobDataMap
            
            // Try to extract the Job ID from the job data map (for dynamic jobs)
            Long jobId = extractJobId(mergedJobDataMap)
            Job job = jobId ? Job.get(jobId) : null
            
            def history = new JobExecutionHistory(
                job: job,
                jobName: jobDetail.key.name,
                jobGroup: jobDetail.key.group,
                triggerName: trigger?.key?.name,
                triggerGroup: trigger?.key?.group,
                fireInstanceId: context.fireInstanceId,
                startTime: context.fireTime,
                scheduledFireTime: context.scheduledFireTime,
                actualFireTime: context.fireTime,
                recovering: context.recovering,
                refireCount: context.refireCount,
                status: status
            )
            
            if (history.save(flush: true, failOnError: false)) {
                log.trace("Created job execution history record: ${history.id}")
                return history.id
            } else {
                log.warn("Failed to save job execution history: ${history.errors}")
                return null
            }
        } catch (Exception e) {
            log.error("Error creating job execution history record: ${e.message}", e)
            return null
        }
    }

    /**
     * Update an existing JobExecutionHistory record with completion status
     */
    @Transactional
    private void updateHistoryRecord(Long historyId, JobExecutionStatus status, JobExecutionException jobException) {
        try {
            def history = JobExecutionHistory.get(historyId)
            if (history) {
                history.status = status
                history.endTime = new Date()
                history.calculateDuration()
                
                if (jobException != null) {
                    history.errorMessage = truncateMessage(jobException.message, 4000)
                    history.exceptionClass = jobException.class.name
                    
                    // Include root cause if available
                    if (jobException.cause != null) {
                        history.errorMessage = truncateMessage(
                            "${jobException.message} | Caused by: ${jobException.cause.message}",
                            4000
                        )
                        history.exceptionClass = jobException.cause.class.name
                    }
                }
                
                history.save(flush: true, failOnError: false)
                log.trace("Updated job execution history record: ${historyId}, status: ${status}, duration: ${history.durationMs}ms")
            }
        } catch (Exception e) {
            log.error("Error updating job execution history record ${historyId}: ${e.message}", e)
        }
    }

    /**
     * Update an existing JobExecutionHistory record with exception details
     */
    @Transactional
    private void updateHistoryRecordWithException(Long historyId, JobExecutionException jobException) {
        try {
            def history = JobExecutionHistory.get(historyId)
            if (history && jobException != null) {
                history.errorMessage = truncateMessage(jobException.message, 4000)
                history.exceptionClass = jobException.class.name
                history.save(flush: true, failOnError: false)
            }
        } catch (Exception e) {
            log.error("Error updating job execution history with exception ${historyId}: ${e.message}", e)
        }
    }

    /**
     * Extract the Job ID from the job data map
     */
    private Long extractJobId(def jobDataMap) {
        if (jobDataMap == null) return null
        
        try {
            // Dynamic jobs store the job ID in the data map
            def jobIdStr = jobDataMap.get(DSLJob.JOB_ID)
            if (jobIdStr) {
                return Long.parseLong(jobIdStr.toString())
            }
        } catch (Exception e) {
            log.trace("Could not extract job ID from data map: ${e.message}")
        }
        return null
    }

    /**
     * Truncate a message to the specified maximum length
     */
    private String truncateMessage(String message, int maxLength) {
        if (message == null) return null
        if (message.length() <= maxLength) return message
        return message.substring(0, maxLength - 3) + "..."
    }
}
