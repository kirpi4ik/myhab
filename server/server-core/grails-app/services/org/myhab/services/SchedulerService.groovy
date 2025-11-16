package org.myhab.services

import org.myhab.domain.job.Job
import org.myhab.domain.job.JobState
import org.myhab.jobs.DSLJob
import grails.gorm.transactions.Transactional
import org.quartz.*
import org.quartz.impl.StdScheduler

@Transactional
class SchedulerService {

    StdScheduler quartzScheduler

    /**
     * Schedule a job in Quartz (does not modify job state)
     */
    def scheduleJobInQuartz(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return false
        }
        
        if (!job.cronTriggers || job.cronTriggers.isEmpty()) {
            log.warn("Job ${jobId} has no cron triggers, cannot schedule")
            return false
        }
        
        job.cronTriggers.each { jobTrigger ->
            // Use job ID as the unique identifier for Quartz
            String quartzJobId = "job_${jobId}"
            String quartzTriggerId = "trigger_${jobTrigger.id}"
            String quartzGroupId = "job_group_${jobId}"
            
            // Convert 5-field cron expression to 6-field (Quartz format)
            String cronExpression = normalizeCronExpression(jobTrigger.expression)
            
            JobDetail jobDetails = JobBuilder.newJob(DSLJob.class)
                    .withIdentity(quartzJobId, quartzGroupId)
                    .withDescription(job.description)
                    .storeDurably()
                    .requestRecovery()
                    .usingJobData(DSLJob.JOB_ID, job.id) // Keep UID for backward compatibility in job execution
                    .usingJobData("jobId", jobId)
                    .build()
                    
            def trigger = TriggerBuilder.newTrigger()
                    .withIdentity(quartzTriggerId, quartzGroupId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .withDescription(job.description)
                    .startAt(DateBuilder.futureDate(10, DateBuilder.IntervalUnit.SECOND))
                    .build()
                    
            TriggerKey triggerKey = TriggerKey.triggerKey(quartzTriggerId, quartzGroupId)
            
            if (quartzScheduler.checkExists(triggerKey)) {
                quartzScheduler.rescheduleJob(triggerKey, trigger)
            } else {
                try {
                    quartzScheduler.scheduleJob(jobDetails, trigger)
                } catch (ObjectAlreadyExistsException alreadyExistsException) {
                    quartzScheduler.resumeJob(jobDetails.key)
                    log.warn(alreadyExistsException.message)
                }
            }
        }
        
        log.info("Job ${jobId} scheduled in Quartz")
        return true
    }

    /**
     * Schedule a job and set state to ACTIVE (legacy method for backward compatibility)
     */
    def scheduleJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        // Set job state to ACTIVE
        job.state = JobState.ACTIVE
        job.save(flush: true)
        
        // Schedule in Quartz
        scheduleJobInQuartz(jobId)
        
        log.info("Job ${jobId} scheduled and set to ACTIVE state")
    }

    /**
     * Unschedule a job from Quartz (does not modify job state)
     */
    def unscheduleJobFromQuartz(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return false
        }
        
        String quartzGroupId = "job_group_${jobId}"
        boolean unscheduled = false
        job.cronTriggers.each { jobTrigger ->
            String quartzTriggerId = "trigger_${jobTrigger.id}"
            TriggerKey triggerKey = TriggerKey.triggerKey(quartzTriggerId, quartzGroupId)
            if (quartzScheduler.checkExists(triggerKey)) {
                quartzScheduler.unscheduleJob(triggerKey)
                unscheduled = true
            }
        }
        
        if (unscheduled) {
            log.info("Job ${jobId} unscheduled from Quartz")
        }
        return unscheduled
    }

    /**
     * Unschedule a job and set state to DISABLED (legacy method for backward compatibility)
     */
    def unscheduleJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        // Unschedule from Quartz first
        unscheduleJobFromQuartz(jobId)
        
        // Set job state to DISABLED
        job.state = JobState.DISABLED
        job.save(flush: true)
        
        log.info("Job ${jobId} unscheduled and set to DISABLED state")
    }

    def pauseJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        if (job.state == JobState.ACTIVE) {
            String quartzGroupId = "job_group_${jobId}"
            job.cronTriggers.each { jobTrigger ->
                String quartzTriggerId = "trigger_${jobTrigger.id}"
                quartzScheduler.pauseTrigger(TriggerKey.triggerKey(quartzTriggerId, quartzGroupId))
            }
        }
    }

    def deleteJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        String quartzJobId = "job_${jobId}"
        String quartzGroupId = "job_group_${jobId}"
        
        if (quartzScheduler.checkExists(JobKey.jobKey(quartzJobId, quartzGroupId))) {
            quartzScheduler.deleteJob(JobKey.jobKey(quartzJobId, quartzGroupId))
        }
    }

    def interrupt(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        if (job.state == JobState.ACTIVE) {
            String quartzJobId = "job_${jobId}"
            String quartzGroupId = "job_group_${jobId}"
            quartzScheduler.interrupt(JobKey.jobKey(quartzJobId, quartzGroupId))
        }
    }

    def triggerJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        if (job.state == JobState.ACTIVE) {
            String quartzJobId = "job_${jobId}"
            String quartzGroupId = "job_group_${jobId}"
            JobKey jobKey = JobKey.jobKey(quartzJobId, quartzGroupId)
            
            // Check if job exists in Quartz, if not, schedule it first
            if (!quartzScheduler.checkExists(jobKey)) {
                log.info("Job ${jobId} not scheduled in Quartz, scheduling it first")
                scheduleJobInQuartz(jobId)
            }
            
            // Now trigger the job
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.triggerJob(jobKey)
                log.info("Job ${jobId} triggered successfully")
            } else {
                log.warn("Failed to schedule job ${jobId} before triggering")
                throw new IllegalStateException("Job ${jobId} could not be scheduled")
            }
        } else {
            log.warn("Job ${jobId} is not ACTIVE, cannot trigger")
            throw new IllegalStateException("Job ${jobId} must be ACTIVE to trigger")
        }
    }

    /**
     * Start all ACTIVE jobs in Quartz scheduler
     * Called on application startup to restore scheduled jobs
     */
    def startAll() {
        def activeJobs = Job.findAllByState(JobState.ACTIVE)
        log.info("Found ${activeJobs.size()} ACTIVE jobs to schedule")
        
        int successCount = 0
        int failureCount = 0
        
        activeJobs.each { job ->
            try {
                // Only schedule in Quartz, don't modify job state (already ACTIVE)
                if (scheduleJobInQuartz(job.id)) {
                    successCount++
                } else {
                    failureCount++
                }
            } catch (Exception e) {
                log.error("Failed to schedule job ${job.id} (${job.name}): ${e.message}", e)
                failureCount++
            }
        }
        
        log.info("Scheduler startup complete: ${successCount} jobs scheduled successfully, ${failureCount} failed")
    }

    def resumeAll() {
        quartzScheduler.resumeAll()
    }

    def shutdown() {
        quartzScheduler.shutdown()
    }

    /**
     * Normalize cron expression to Quartz format (6 or 7 fields)
     * Converts 5-field Unix cron format to 6-field Quartz format by adding seconds field
     * Note: Quartz requires that either day-of-month OR day-of-week must be '?' (not both '*')
     */
    private String normalizeCronExpression(String expression) {
        if (!expression) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty")
        }
        
        // Remove leading/trailing whitespace
        expression = expression.trim()
        
        // Split by whitespace
        def fields = expression.split("\\s+")
        
        if (fields.length == 5) {
            // Convert 5-field format (minute hour day month weekday) to 6-field format (second minute hour day month weekday)
            // Format: minute hour day-of-month month day-of-week
            // Quartz format: second minute hour day-of-month month day-of-week
            // Quartz rule: day-of-month and day-of-week cannot both be '*', one must be '?'
            
            String minute = fields[0]
            String hour = fields[1]
            String dayOfMonth = fields[2]
            String month = fields[3]
            String dayOfWeek = fields[4]
            
            // If both day-of-month and day-of-week are '*', change day-of-week to '?'
            if (dayOfMonth == '*' && dayOfWeek == '*') {
                dayOfWeek = '?'
            }
            // If day-of-month is '*' and day-of-week is not '*', change day-of-month to '?'
            else if (dayOfMonth == '*' && dayOfWeek != '*') {
                dayOfMonth = '?'
            }
            // If day-of-week is '*' and day-of-month is not '*', change day-of-week to '?'
            else if (dayOfWeek == '*' && dayOfMonth != '*') {
                dayOfWeek = '?'
            }
            
            // Build 6-field Quartz expression: second minute hour day-of-month month day-of-week
            return "0 ${minute} ${hour} ${dayOfMonth} ${month} ${dayOfWeek}"
        } else if (fields.length == 6 || fields.length == 7) {
            // Already in Quartz format (6 or 7 fields)
            // Validate that day-of-month and day-of-week don't both have values
            if (fields.length >= 6) {
                String dayOfMonth = fields[3]  // 4th field (0-indexed: 3)
                String dayOfWeek = fields[5]  // 6th field (0-indexed: 5)
                
                // Quartz rule: day-of-month and day-of-week cannot both be '*', one must be '?'
                // If both are '*', change day-of-week to '?'
                if (dayOfMonth == '*' && dayOfWeek == '*') {
                    fields[5] = '?'
                    return fields.join(' ')
                }
                // If day-of-month is '*' and day-of-week is not '*', change day-of-month to '?'
                else if (dayOfMonth == '*' && dayOfWeek != '*' && dayOfWeek != '?') {
                    fields[3] = '?'
                    return fields.join(' ')
                }
                // If day-of-week is '*' and day-of-month is not '*', change day-of-week to '?'
                else if (dayOfWeek == '*' && dayOfMonth != '*' && dayOfMonth != '?') {
                    fields[5] = '?'
                    return fields.join(' ')
                }
            }
            return expression
        } else {
            throw new IllegalArgumentException("Invalid cron expression format: '${expression}'. Expected 5, 6, or 7 fields, got ${fields.length}")
        }
    }
}