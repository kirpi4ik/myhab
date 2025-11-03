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

    def scheduleJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        // Set job state to ACTIVE
        job.state = JobState.ACTIVE
        job.save(flush: true)
        
        job.cronTriggers.each { jobTrigger ->
            // Use job ID as the unique identifier for Quartz
            String quartzJobId = "job_${jobId}"
            String quartzTriggerId = "trigger_${jobTrigger.id}"
            String quartzGroupId = "job_group_${jobId}"
            
            JobDetail jobDetails = JobBuilder.newJob(DSLJob.class)
                    .withIdentity(quartzJobId, quartzGroupId)
                    .withDescription(job.description)
                    .storeDurably()
                    .requestRecovery()
                    .usingJobData(DSLJob.JOB_UID, job.uid) // Keep UID for backward compatibility in job execution
                    .usingJobData("jobId", jobId)
                    .build()
                    
            def trigger = TriggerBuilder.newTrigger()
                    .withIdentity(quartzTriggerId, quartzGroupId)
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobTrigger.expression))
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
        
        log.info("Job ${jobId} scheduled and set to ACTIVE state")
    }

    def unscheduleJob(Long jobId) {
        def job = Job.get(jobId)
        if (!job) {
            log.warn("Job with id ${jobId} not found")
            return
        }
        
        // Set job state to DISABLED
        job.state = JobState.DISABLED
        job.save(flush: true)
        
        String quartzGroupId = "job_group_${jobId}"
        job.cronTriggers.each { jobTrigger ->
            String quartzTriggerId = "trigger_${jobTrigger.id}"
            quartzScheduler.unscheduleJob(TriggerKey.triggerKey(quartzTriggerId, quartzGroupId))
        }
        
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
            quartzScheduler.triggerJob(JobKey.jobKey(quartzJobId, quartzGroupId))
        }
    }

    def startAll() {
        Job.findAllByState(JobState.ACTIVE).each { job ->
            scheduleJob(job.id)
        }
    }

    def resumeAll() {
        quartzScheduler.resumeAll()
    }

    def shutdown() {
        quartzScheduler.shutdown()
    }
}