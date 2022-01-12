package org.myhab.services.dsl.action

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Transactional
@Slf4j
class WakeupService implements DslCommand {

  @Override
  def execute(args) {
    log.debug("Wakeup: "+args)
  }
}
