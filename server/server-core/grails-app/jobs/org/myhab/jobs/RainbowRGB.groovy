package org.myhab.jobs

import grails.events.EventPublisher
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

class RainbowRGB implements Job, EventPublisher {
    // DISABLED: Grails auto-scheduling conflicts with SchedulerService
    // Jobs are now managed via SchedulerService and database-backed triggers
    /*
    static triggers = {
        simple name: 'rainbowRGBColors', repeatInterval: TimeUnit.SECONDS.toMillis(80)
    }
    */

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {

    }
}
