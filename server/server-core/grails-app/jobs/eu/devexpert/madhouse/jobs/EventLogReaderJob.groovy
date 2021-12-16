package eu.devexpert.madhouse.jobs

import groovy.util.logging.Slf4j
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

@Slf4j
@DisallowConcurrentExecution
class EventLogReaderJob implements Job {
  static triggers = {
    simple repeatInterval: 50000 // execute job once in 5 seconds
  }
  def eventRouter

  def execute() {

//    eventService.saveLog()
  }

  @Override
  void execute(JobExecutionContext context) throws JobExecutionException {
    log.debug("###################################-${context.getMergedJobDataMap().get("key")}")
  }
}
