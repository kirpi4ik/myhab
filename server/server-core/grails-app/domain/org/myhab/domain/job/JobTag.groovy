package org.myhab.domain.job

class JobTag {
  String name
  Set<Job> jobs

  static hasMany = [jobs: Job]
  static belongsTo = [Job]

  static mapping = {
    table '`jobs_tags`'
    jobs joinTable: [name: "jobs_tags_join", key: 'tag_id']
  }

  static graphql = true
}
