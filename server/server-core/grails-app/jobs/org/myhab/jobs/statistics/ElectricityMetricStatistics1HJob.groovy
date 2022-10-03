package org.myhab.jobs.statistics


import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.quartz.*

import java.util.concurrent.TimeUnit

@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
class ElectricityMetricStatistics1HJob implements Job{
    def statisticsService
    static triggers = {
        simple repeatInterval: TimeUnit.MINUTES.toMillis(5)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def now = DateTime.now()
        statisticsService.saveActivePowerStatistics("agg_active_power_60min", now.minusMinutes(60).toDate(), now.minusMinutes(50).toDate())
    }

}
