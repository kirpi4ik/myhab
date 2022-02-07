package org.myhab.jobs

import groovy.util.logging.Slf4j
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
class ConfigSyncJob implements Job {
    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }
    def configProvider

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        configProvider.asyncLoad({ loaded ->
            if (loaded) {
                log.debug "Configuration updated"
            }
        })
    }
}
