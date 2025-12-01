package org.myhab.jobs

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.ConfigKey
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.init.cache.CacheMap
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

/**
 * SwitchOFF peripheral after some timeout, also check if there is some peripheral in status ON but without cached expiration
 */
@Slf4j
@DisallowConcurrentExecution
@Transactional
class SwitchOFFOnTimeoutJob implements Job, EventPublisher {
    HazelcastInstance hazelcastInstance;

    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.switchOffOnTimeout.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.switchOffOnTimeout.interval', Integer) ?: 30
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "SwitchOFFOnTimeoutJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "SwitchOFFOnTimeoutJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.switchOffOnTimeout.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("SwitchOFFOnTimeoutJob is DISABLED via configuration, skipping execution")
            return
        }
        checkCacheAndSwitchOffAfterTimeout(context)
        checkOnPeripheralAndSetTimeoutValueIfNeeded(context)
    }

    void checkOnPeripheralAndSetTimeoutValueIfNeeded(JobExecutionContext jobExecutionContext) {
        def portsWithON = DevicePort.where { value ==~ 'ON' }.list()
        
        portsWithON.each { port ->
            boolean cached = false
            hazelcastInstance.getMap(CacheMap.EXPIRE.name).entrySet().each { candidateForExpiration ->
                if (candidateForExpiration.key == String.valueOf(port.id)) {
                    cached = true
                    return true
                }
            }
            
            if (!cached) {
                def peripheral = port.peripherals[0]
                if (peripheral != null) {
                    def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
                    
                        if (config != null) {
                            def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(config.value)).toDate().time
                            hazelcastInstance.getMap(CacheMap.EXPIRE.name).put(String.valueOf(port.id), [expireOn: expireInMs, peripheralId: peripheral.id])
                            log.debug("Scheduled job created cache for port ${port.id}, expires at ${new Date(expireInMs)}, peripheral ${peripheral.id}, timeout ${config.value}s")
                        }
                }
            }
        }
    }

    def checkCacheAndSwitchOffAfterTimeout(JobExecutionContext context) {
        hazelcastInstance.getMap(CacheMap.EXPIRE.name).entrySet().each { candidateForExpiration ->
            def objToExpire = candidateForExpiration?.value
            def now = DateTime.now()
            if (objToExpire?.peripheralId != null && now.isAfter(objToExpire?.expireOn)) {
                publish(TopicName.EVT_LIGHT.id(), [
                        "p0": TopicName.EVT_LIGHT.id(),
                        "p1": EntityType.PERIPHERAL.name(),
                        "p2": objToExpire?.peripheralId,
                        "p3": "timeout",
                        "p4": "off",
                        "p6": "system"
                ])
                hazelcastInstance.getMap(CacheMap.EXPIRE.name).remove(String.valueOf(candidateForExpiration?.key))
            }
        }
    }
}
