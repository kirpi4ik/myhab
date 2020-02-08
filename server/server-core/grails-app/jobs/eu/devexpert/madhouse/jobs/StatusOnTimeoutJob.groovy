package eu.devexpert.madhouse.jobs

import com.hazelcast.core.HazelcastInstance
import eu.devexpert.madhouse.init.cache.CacheMap
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.joda.time.DateTime
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 *
 */
@Transactional
class StatusOnTimeoutJob implements Job, EventPublisher {
    HazelcastInstance hazelcastInstance;

    static triggers = {
        simple repeatInterval: 10000 // execute job once in 5 seconds
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        println "CHECK CACHE : ${hazelcastInstance.getMap(CacheMap.EXPIRE)}"
        hazelcastInstance.getMap(CacheMap.EXPIRE).entrySet().each { candidateForExpiration ->
            println "CACHE: ${candidateForExpiration.key} | ${candidateForExpiration.value}"
            def value = candidateForExpiration?.value
            println "${candidateForExpiration?.key} | ${value?.expireOn} | ${value?.portUid}"
            def now = DateTime.now()
            if (now.isAfter(value?.expireOn)) {
                publish("evt_light", [
                        "p0": "evt_light",
                        "p1": "PERIPHERAL",
                        "p2": value?.portUid,
                        "p3": "timout",
                        "p4": "off",
                        "p6": "system"
                ])
                hazelcastInstance.getMap(CacheMap.EXPIRE).remove(candidateForExpiration?.key)
            }
        }
    }
}
