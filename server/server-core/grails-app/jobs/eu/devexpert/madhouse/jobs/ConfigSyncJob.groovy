package eu.devexpert.madhouse.jobs

import groovy.util.logging.Slf4j
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

@Slf4j
@DisallowConcurrentExecution
class ConfigSyncJob implements Job {
    static triggers = {
        simple repeatInterval: 5000 // execute job once in 5 seconds
    }
    def configProvider

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        configProvider.asyncLoad({ loaded ->
            if (!loaded) {
                log.error "something is wrong with config"
            }
        })
    }
}
