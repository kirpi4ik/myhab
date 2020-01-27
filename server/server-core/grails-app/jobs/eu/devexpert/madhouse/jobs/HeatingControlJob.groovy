package eu.devexpert.madhouse.jobs

import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 *
 */
@Transactional
class HeatingControlJob implements Job, EventPublisher {
  @Override
  void execute(JobExecutionContext context) throws JobExecutionException {

  }
}
