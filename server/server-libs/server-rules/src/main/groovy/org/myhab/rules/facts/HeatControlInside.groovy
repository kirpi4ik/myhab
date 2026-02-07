package org.myhab.rules.facts


import groovy.util.logging.Slf4j
import org.jeasy.rules.api.Facts
import org.jeasy.rules.core.BasicRule

@Slf4j
class HeatControlInside extends BasicRule {
    String name

    HeatControlInside(String name) {
        this.name = name
    }

    public boolean evaluate(Facts facts) {
        return true;
    }

    public void execute(Facts facts) throws Exception {
//        heatService.heatOn(facts["zoneId"])
        log.debug "XYZ:::$name::" + facts[name]
    }

}
