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
    public static final String JOB_UID = "jobId"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def jobUid = context.getMergedJobDataMap().get(JOB_UID)
        def job = org.myhab.domain.job.Job.findByUid(jobUid)

        ApplicationContext ctx = Holders.grailsApplication.mainContext
        DslService dslService = (DslService) ctx.getBean("dslService");

        if (job != null && job.scenario != null) {
            dslService.execute(job.scenario.body)
        }
    }
}
