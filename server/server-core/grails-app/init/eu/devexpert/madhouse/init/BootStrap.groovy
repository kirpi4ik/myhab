package eu.devexpert.madhouse.init

import eu.devexpert.madhouse.init.cache.CacheMap
import eu.devexpert.rules.facts.HeatControlInside
import org.jeasy.rules.api.Facts
import org.jeasy.rules.api.Rules

class BootStrap {
    def alertService
    def hazelcastInstance;
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
//        hazelcastInstance.getMap(CacheMap.EXPIRE).put("key", "11111111111111111111");
//        println ":::::::::"+hazelcastInstance.getMap(CacheMap.EXPIRE).get("key");
    }
    def destroy = {
//        schedulerService.shutdown()
    }

}
