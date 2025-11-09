package org.myhab.jobs


import grails.gorm.transactions.Transactional
import grails.util.Holders
import org.myhab.services.dsl.DslService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.context.ApplicationContext

/**
 *
 */
@Transactional
class DSLJob implements Job {
    public static final String JOB_ID = "jobId"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def jobId = context.getMergedJobDataMap().get(JOB_ID)
        def job = org.myhab.domain.job.Job.findById(jobId)

        ApplicationContext ctx = Holders.grailsApplication.mainContext
        DslService dslService = (DslService) ctx.getBean("dslService");

        if (job != null && job.scenario != null) {
            dslService.execute(job.scenario.body)
        }
    }
}
