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

    def scheduleJob(String jobUid) {
        def job = Job.findByUid(jobUid)
        if (job.state == JobState.ACTIVE) {
            job.cronTriggers.each { jobTrigger ->
                JobDetail jobDetails = JobBuilder.newJob(DSLJob.class)
                        .withIdentity(jobUid)
                        .withDescription(job.description)
                        .storeDurably()
                        .requestRecovery()
                        .usingJobData(DSLJob.JOB_UID, jobUid)
                        .build()
                def trigger = TriggerBuilder.newTrigger().withIdentity(TriggerKey.triggerKey(jobTrigger.uid, job.uid))
                        .withSchedule(CronScheduleBuilder.cronSchedule(jobTrigger.expression))
                        .withDescription(job.description)
                        .startAt(DateBuilder.futureDate(10, DateBuilder.IntervalUnit.SECOND))
                        .build()
                if (quartzScheduler.checkExists(TriggerKey.triggerKey(jobTrigger.uid, job.uid))) {
                    quartzScheduler.rescheduleJob(TriggerKey.triggerKey(jobTrigger.uid, job.uid), trigger)
                } else {
                    if (!quartzScheduler.checkExists(TriggerKey.triggerKey(jobTrigger.uid, job.uid))) {
                        try {
                            quartzScheduler.scheduleJob(jobDetails, trigger)
                        } catch (ObjectAlreadyExistsException alreadyExistsException) {
                            quartzScheduler.resumeJob(jobDetails.key)
                            log.warn(alreadyExistsException.message)
                        }
                    }
                }
            }
        }
    }

    def unscheduleJob(String jobUid) {
        def job = Job.findByUid(jobUid)
        job.cronTriggers.each { jobTrigger ->
            quartzScheduler.unscheduleJob(TriggerKey.triggerKey(jobTrigger.uid, jobUid))
        }
    }


    def pauseJob(String jobUid) {
        def job = Job.findByUid(jobUid)
        if (job.state == JobState.ACTIVE) {
            job.cronTriggers.each { jobTrigger ->
                quartzScheduler.pauseJob(TriggerKey.triggerKey(jobTrigger.uid, jobUid))
            }
        }
    }

    def deleteJob(String jobUid) {
        def job = Job.findByUid(jobUid)
        if (quartzScheduler.checkExists(JobKey.jobKey(jobUid))) {
            quartzScheduler.deleteJob(JobKey.jobKey(jobUid))
        }

    }

    def interrupt(String jobUid) {
        def job = Job.findByUid(jobUid)
        if (job.state == JobState.ACTIVE) {
            job.cronTriggers.each { jobTrigger ->
                quartzScheduler.interrupt(TriggerKey.triggerKey(jobTrigger.uid, jobUid))
            }
        }
    }

    def triggerJob(String jobUid) {
        def job = Job.findByUid(jobUid)
        if (job.state == JobState.ACTIVE) {
            job.cronTriggers.each { jobTrigger ->
                quartzScheduler.triggerJob(JobKey.jobKey(jobUid))
            }
        }
    }

    def startAll() {
        Job.findByState(JobState.ACTIVE).each { job ->
            scheduleJob(job.uid)
        }
    }

    def resumeAll() {
        quartzScheduler.resumeAll()
    }

    def shutdown() {
        quartzScheduler.shutdown()
    }
}