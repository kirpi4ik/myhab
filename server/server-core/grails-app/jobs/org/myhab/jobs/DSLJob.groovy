package org.myhab.jobs


import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.services.dsl.DslService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.context.ApplicationContext

/**
 * DSL Job executor for scheduled scenario execution
 */
@Slf4j
@Transactional
class DSLJob implements Job {
    public static final String JOB_ID = "jobId"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def jobId = context.getMergedJobDataMap().get(JOB_ID)
        def job = org.myhab.domain.job.Job.findById(jobId)

        if (job == null) {
            log.error("Job not found for id: ${jobId}")
            throw new JobExecutionException("Job not found for id: ${jobId}")
        }

        if (job.scenario == null) {
            log.warn("Job ${jobId} (${job.name}) has no scenario attached, skipping execution")
            return
        }

        log.info("Executing DSL Job ${jobId} (${job.name}) with scenario: ${job.scenario.name}")

        try {
            ApplicationContext ctx = Holders.grailsApplication.mainContext
            DslService dslService = (DslService) ctx.getBean("dslService")
            
            dslService.execute(job.scenario.body)
            
            log.info("Job ${jobId} (${job.name}) executed successfully")
        } catch (Exception e) {
            log.error("Error executing DSL Job ${jobId} (${job.name}): ${e.message}", e)
            
            // Create JobExecutionException with detailed error information
            JobExecutionException jobException = new JobExecutionException(
                "Job ${jobId} (${job.name}) execution failed: ${e.message}", 
                e
            )
            
            // Don't automatically unschedule the job on error
            jobException.setUnscheduleAllTriggers(false)
            
            throw jobException
        }
    }
}
