package org.myhab.jobs.statistics


import grails.util.Holders
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.quartz.*

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class ElectricityMetricStatistics24HJob implements Job{
    def statisticsService
    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.electricityMetricStatistics24H.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.electricityMetricStatistics24H.interval', Integer) ?: 86400
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            println "ElectricityMetricStatistics24HJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            println "ElectricityMetricStatistics24HJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.electricityMetricStatistics24H.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("ElectricityMetricStatistics24HJob is DISABLED via configuration, skipping execution")
            return
        }
        def now = DateTime.now()
        statisticsService.saveActivePowerStatistics("agg_active_power_24h", now.minusDays(1).toDate(), now.minusDays(1).plusHours(1).toDate())
    }

}
