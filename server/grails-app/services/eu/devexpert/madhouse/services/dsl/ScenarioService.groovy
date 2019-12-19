package eu.devexpert.madhouse.services.dsl

import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.services.dsl.action.DslCommand
import eu.devexpert.madhouse.services.dsl.action.PowerOut
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.springframework.context.ApplicationContext

@Slf4j
@Transactional
class ScenarioService {

    def getIsEvening() {
        return true
    }

    def lightsOn(args) {
        args.action = PortAction.ON
        return new PowerOut(args)
    }

    def lightsOff(args) {
        args.action = PortAction.OFF
        return new PowerOut(args)
    }

    def lightsReverse(args) {
        args.action = PortAction.REVERSE
        return new PowerOut(args)
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