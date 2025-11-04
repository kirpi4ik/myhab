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
class EventLogReaderJob implements Job {
  static triggers = {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.eventLogReader.enabled', Boolean)
    def interval = config?.getProperty('quartz.jobs.eventLogReader.interval', Integer) ?: 60
    
    if (enabled == null) {
      enabled = true  // Default to enabled for backward compatibility
    }
    
    if (enabled) {
      println "EventLogReaderJob: ENABLED - Registering trigger with interval ${interval}s"
      simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
    } else {
      println "EventLogReaderJob: DISABLED - Not registering trigger"
    }
  }
  def eventRouter

  def execute() {

//    eventService.saveLog()
  }

  @Override
  void execute(JobExecutionContext context) throws JobExecutionException {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.eventLogReader.enabled', Boolean)
    
    if (enabled == null) {
      enabled = true
    }
    
    if (!enabled) {
      log.info("EventLogReaderJob is DISABLED via configuration, skipping execution")
      return
    }
//    log.debug("###################################-${context.getMergedJobDataMap().get("key")}")
  }
}
