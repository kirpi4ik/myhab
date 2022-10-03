package org.myhab.jobs.statistics


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
        simple repeatInterval: TimeUnit.MINUTES.toMillis(30)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def now = DateTime.now()
        statisticsService.saveActivePowerStatistics("agg_active_power_24h", now.minusDays(1).toDate(), now.minusDays(1).plusHours(1).toDate())
    }

}
