package org.myhab.jobs

import grails.util.Holders
import groovy.util.logging.Slf4j
import grails.gorm.transactions.Transactional
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.myhab.config.ConfigProvider
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.TimeUnit

/**
 * Configuration Synchronization Job
 *
 * Asynchronously reloads application configuration from the GIT.
 * Ensures configuration changes are picked up without requiring application restart.
 *
 * This job is now managed by Spring IoC via `resources.groovy` and configured in `application.yml`.
 * It is no longer auto-registered by the Grails Quartz plugin.
 *
 * Configuration:
 * - quartz.jobs.configSync.enabled: true/false (default: true)
 * - quartz.jobs.configSync.interval: seconds (default: 60)
 */
@Slf4j
@DisallowConcurrentExecution
@Transactional
class ConfigSyncJob implements Job {
    
    @Autowired
    ConfigProvider configProvider

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        configProvider.asyncLoad({ loaded ->
            if (loaded) {
                log.debug "Configuration updated"
            }
        })
    }
}
