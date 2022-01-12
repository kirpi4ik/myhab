package org.myhab.services.dsl

import org.myhab.domain.device.port.PortAction
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.services.dsl.action.DslCommand
import org.springframework.context.ApplicationContext

@Slf4j
@Transactional
class ScenarioService {

    def powerService

    def getIsEvening() {
        return true
    }

    def lightsOn(args) {
        args.action = PortAction.ON
        powerService.execute(args)
    }

    def lightsOff(args) {
        args.action = PortAction.OFF
        powerService.execute(args)
    }

    def lightsReverse(args) {
        args.action = PortAction.TOGGLE
        powerService.execute(args)
    }

    def pause(args) {
        log.debug(args)
        Thread.sleep(args)
    }

    def methodMissing(String methodName, args) {
        ApplicationContext ctx = Holders.grailsApplication.mainContext
        DslCommand dslCommand = (DslCommand) ctx.getBean("${methodName}Service");
        dslCommand.execute(args);

        log.debug("missing method : ${methodName}")
        return dslCommand
    }

/*  def propertyMissing(String paramName) {
    log.debug("missing property: ${paramName}")
    return new PowerOut()
  }*/

}
