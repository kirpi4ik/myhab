package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.quartz.TriggerKey

/**
 * CronTrigger represents a cron-based trigger for a Job.
 * 
 * <p>When a trigger is deleted, it's automatically unscheduled from Quartz.
 * When a trigger expression is modified, the parent job should be rescheduled.</p>
 */
@Slf4j
class CronTrigger extends BaseEntity {
  String expression
  String description
  Job job

  static belongsTo = [Job]
  static hasOne = [job: Job]

  static mapping = {
    table '`job_triggers_cron`'
  }

  static constraints = {
    expression nullable: false
    description nullable: true, maxSize: 500
  }

  static graphql = true

  /**
   * Before deleting a cron trigger, unschedule it from Quartz
   * 
   * <p>This only handles individual trigger deletion. When multiple triggers are updated
   * as part of a job update, the Job.afterUpdate() method handles rescheduling
   * efficiently in a single operation.</p>
   */
  void beforeDelete() {
    try {
      if (id && job?.id) {
        def quartzScheduler = Holders.grailsApplication.mainContext.getBean('quartzScheduler')
        String quartzTriggerId = "trigger_${id}"
        String quartzGroupId = "job_group_${job.id}"
        
        TriggerKey triggerKey = TriggerKey.triggerKey(quartzTriggerId, quartzGroupId)
        if (quartzScheduler.checkExists(triggerKey)) {
          quartzScheduler.unscheduleJob(triggerKey)
          log.info("Unscheduled cron trigger ${id} from Quartz for job ${job.id}")
        }
      }
    } catch (Exception e) {
      log.warn("Failed to unschedule cron trigger ${id} from Quartz: ${e.message}")
      // Don't throw exception - allow deletion to proceed
    }
  }
}
