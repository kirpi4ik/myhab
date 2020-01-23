package madhouse.v2

import eu.devexpert.madhouse.alert.Alert
import eu.devexpert.madhouse.alert.AlertPriority
import eu.devexpert.rules.facts.HeatControlInside
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules
import org.jeasy.rules.api.RulesEngine
import org.jeasy.rules.core.InferenceRulesEngine

class BootStrap {
    def alertService

    def schedulerService
    def init = { servletContext ->
//        schedulerService.startAll()
//        def var = new Alert(AlertPriority.P1, "helo", "description")
//        alertService.sendAlert(var)

        List<String> list = new ArrayList<>()
        list << "xxx"
        list << "yyy"
        list << "zzz"
        list << "www"

        Rules rules = new Rules();
        Facts facts = new Facts();
        list.each { it ->
            println "Start::: $it"
            facts.put(it, true);
            rules.register(new HeatControlInside(it));
        }
//        new InferenceRulesEngine().fire(rules, facts);

    }
    def destroy = {
//        schedulerService.shutdown()
    }

}
