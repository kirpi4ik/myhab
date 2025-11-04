package org.myhab.jobs.statistics


import grails.util.Holders
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.quartz.*

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class ElectricityMetricStatistics1MonthJob implements Job{
    def statisticsService
    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.electricityMetricStatistics1Month.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.electricityMetricStatistics1Month.interval', Integer) ?: 2592000
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "ElectricityMetricStatistics1MonthJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "ElectricityMetricStatistics1MonthJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.electricityMetricStatistics1Month.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("ElectricityMetricStatistics1MonthJob is DISABLED via configuration, skipping execution")
            return
        }
        def now = DateTime.now()
        def firstDayOfMonth = new DateTime(now.year, now.monthOfYear, 1, 0, 0)
        statisticsService.saveActivePowerStatistics("agg_active_power_1month", firstDayOfMonth.toDate(), firstDayOfMonth.plusHours(1).toDate())
    }

}
