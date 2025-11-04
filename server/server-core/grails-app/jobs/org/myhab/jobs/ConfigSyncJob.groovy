package org.myhab.jobs

import grails.util.Holders
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
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.configSync.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.configSync.interval', Integer) ?: 60
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "ConfigSyncJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "ConfigSyncJob: DISABLED - Not registering trigger"
        }
    }
    def configProvider

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.configSync.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("ConfigSyncJob is DISABLED via configuration, skipping execution")
            return
        }
        configProvider.asyncLoad({ loaded ->
            if (loaded) {
                log.debug "Configuration updated"
            }
        })
    }
}
