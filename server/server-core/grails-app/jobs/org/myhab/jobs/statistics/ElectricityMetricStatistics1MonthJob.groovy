package org.myhab.jobs.statistics


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
        simple repeatInterval: TimeUnit.HOURS.toMillis(1)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def now = DateTime.now()
        def firstDayOfMonth = new DateTime(now.year, now.monthOfYear, 1, 0, 0)
        statisticsService.saveActivePowerStatistics("agg_active_power_1month", firstDayOfMonth.toDate(), firstDayOfMonth.plusHours(1).toDate())
    }

}
