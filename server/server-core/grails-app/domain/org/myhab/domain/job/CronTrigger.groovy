package org.myhab.domain.job

import org.myhab.domain.common.BaseEntity

class CronTrigger extends BaseEntity {
  String expression
  Job job

  static belongsTo = [Job]
  static hasOne = [job: Job]

  static mapping = {
    table '`job_triggers_cron`'
  }
}
