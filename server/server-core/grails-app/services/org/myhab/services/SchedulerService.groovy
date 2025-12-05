package org.myhab.services

import org.myhab.domain.job.Job
import org.myhab.domain.job.JobState
import org.myhab.jobs.DSLJob
import grails.gorm.transactions.Transactional
import org.springframework.transaction.annotation.Propagation
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
        
        // Use job ID as the unique identifier for Quartz
        String quartzJobId = "job_${jobId}"
        String quartzGroupId = "job_group_${jobId}"
        JobKey jobKey = JobKey.jobKey(quartzJobId, quartzGroupId)
        
        // Create JobDetail once (outside the loop)
        // NOTE: Don't use .usingJobData() here - job_data is ALWAYS BYTEA and will serialize
        // Instead, store data at trigger level where useProperties=true works
        JobDetail jobDetails = JobBuilder.newJob(DSLJob.class)
                .withIdentity(quartzJobId, quartzGroupId)
                .withDescription(job.description)
                .storeDurably()
                .requestRecovery()
                .build()
        
        // Check if job already exists in Quartz, if not, add it
        boolean jobExists = quartzScheduler.checkExists(jobKey)
        if (!jobExists) {
            log.debug("Adding job ${quartzJobId} to Quartz JobStore...")
            quartzScheduler.addJob(jobDetails, false)
            log.debug("Created job ${quartzJobId} in Quartz")
            
            // Verify it was persisted
            boolean exists = quartzScheduler.checkExists(jobKey)
            log.info("Job ${quartzJobId} exists after add: ${exists}")
        } else {
            log.debug("Job ${quartzJobId} already exists in Quartz")
        }
        
        // Now schedule each trigger
        job.cronTriggers.each { jobTrigger ->
            String quartzTriggerId = "trigger_${jobTrigger.id}"
            
            // Convert 5-field cron expression to 6-field (Quartz format)
            String cronExpression = normalizeCronExpression(jobTrigger.expression)
            
            // Store job data at TRIGGER level where useProperties=true works
            def trigger = TriggerBuilder.newTrigger()
                    .withIdentity(quartzTriggerId, quartzGroupId)
                    .forJob(quartzJobId, quartzGroupId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())  // Don't fire immediately on misfire/reschedule
                    .withDescription(job.description)
                    .usingJobData(DSLJob.JOB_ID, jobId.toString())  // Store at trigger level as String
                    .startAt(DateBuilder.futureDate(10, DateBuilder.IntervalUnit.SECOND))
                    .build()
                    
            TriggerKey triggerKey = TriggerKey.triggerKey(quartzTriggerId, quartzGroupId)
            
            if (quartzScheduler.checkExists(triggerKey)) {
                // Trigger exists, reschedule it
                log.debug("Rescheduling existing trigger ${quartzTriggerId}...")
                quartzScheduler.rescheduleJob(triggerKey, trigger)
                log.debug("Rescheduled trigger ${quartzTriggerId} for job ${jobId}")
            } else {
                // Trigger doesn't exist, schedule it
                log.debug("Scheduling new trigger ${quartzTriggerId}...")
                def scheduledDate = quartzScheduler.scheduleJob(trigger)
                log.debug("Scheduled new trigger ${quartzTriggerId} for job ${jobId}, first fire time: ${scheduledDate}")
                
                // Verify it was persisted
                boolean exists = quartzScheduler.checkExists(triggerKey)
                log.info("Trigger ${quartzTriggerId} exists after schedule: ${exists}")
            }
        }
        
        log.info("Job ${jobId} scheduled in Quartz with ${job.cronTriggers.size()} trigger(s)")
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
     * REQUIRES_NEW: Create a NEW, independent transaction that commits immediately
     * This ensures data is visible to Quartz's background threads after this method completes
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    def startAll() {
        def activeJobs = Job.findAllByState(JobState.ACTIVE)
        log.info("Scheduling ${activeJobs.size()} ACTIVE jobs in Quartz")
        
        int successCount = 0
        int failureCount = 0
        
        activeJobs.each { job ->
            try {
                if (scheduleJobInQuartz(job.id)) {
                    successCount++
                    log.debug("Scheduled job ${job.id} (${job.name}) in Quartz")
                } else {
                    failureCount++
                    log.warn("Failed to schedule job ${job.id} in Quartz")
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
     * Note: Quartz day-of-week is 1-7 (1=SUN), Unix cron is 0-6 (0=SUN)
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
            
            // Convert Unix cron day-of-week (0-6) to Quartz day-of-week (1-7)
            dayOfWeek = convertUnixDayOfWeekToQuartz(dayOfWeek)
            
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
                
                // Convert Unix cron day-of-week (0-6) to Quartz day-of-week (1-7)
                dayOfWeek = convertUnixDayOfWeekToQuartz(dayOfWeek)
                fields[5] = dayOfWeek
                
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
            return fields.join(' ')
        } else {
            throw new IllegalArgumentException("Invalid cron expression format: '${expression}'. Expected 5, 6, or 7 fields, got ${fields.length}")
        }
    }

    /**
     * Convert Unix cron day-of-week (0-6, 0=Sunday) to Quartz day-of-week (1-7, 1=Sunday)
     * Handles ranges, lists, and combinations (e.g., "0-1,3,5")
     * 
     * Examples:
     * - "0" -> "1" (Sunday)
     * - "0-5" -> "1-6" (Sunday to Friday)
     * - "1-5" -> "2-6" (Monday to Friday)
     * - "0,6" -> "1,7" (Sunday and Saturday)
     * - "0-1,3,5" -> "1-2,4,6" (Sunday-Monday, Wednesday, Friday)
     * - "*" -> "*" (unchanged)
     * - "?" -> "?" (unchanged)
     */
    private String convertUnixDayOfWeekToQuartz(String dayOfWeek) {
        if (!dayOfWeek || dayOfWeek == '*' || dayOfWeek == '?') {
            return dayOfWeek
        }
        
        // Split by comma first to handle combinations like "0-1,3,5"
        if (dayOfWeek.contains(',')) {
            def values = dayOfWeek.split(',').collect { segment ->
                convertSingleDaySegment(segment.trim())
            }
            return values.join(',')
        }
        
        // Handle single segment (range or value)
        return convertSingleDaySegment(dayOfWeek)
    }
    
    /**
     * Convert a single day-of-week segment (either a range like "0-5" or a single value like "0")
     */
    private String convertSingleDaySegment(String segment) {
        // Handle ranges (e.g., "0-5", "1-6")
        if (segment.contains('-')) {
            def parts = segment.split('-')
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0])
                    int end = Integer.parseInt(parts[1])
                    // Convert: 0->1, 1->2, 2->3, 3->4, 4->5, 5->6, 6->7
                    return "${start + 1}-${end + 1}"
                } catch (NumberFormatException e) {
                    // Not numeric, return as-is (might be SUN-FRI format)
                    return segment
                }
            }
        }
        
        // Handle single value (e.g., "0", "5")
        try {
            int day = Integer.parseInt(segment)
            return String.valueOf(day + 1)
        } catch (NumberFormatException e) {
            // Not numeric, return as-is (might be SUN, MON, etc.)
            return segment
        }
    }
}