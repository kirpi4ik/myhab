package org.myhab.rules.facts


import org.jeasy.rules.api.Facts
import org.jeasy.rules.core.BasicRule

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
        println "XYZ:::$name::" + facts[name]
    }

}
