package org.myhab.jobs

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.ConfigKey
import org.myhab.domain.EntityType
import org.myhab.domain.MessageLevel
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.TopicName
import org.myhab.init.cache.CacheMap
import org.myhab.services.MegaDriverService
import org.myhab.services.NotificationService
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
    /** After this many unconfirmed OFF attempts, escalate to direct HTTP + admin alert. */
    static final int ESCALATE_AFTER_ATTEMPTS = 3
    /** Retry cadence for an unconfirmed OFF — much shorter than a full timeout cycle. */
    static final int RETRY_DELAY_SEC = 60

    // Typed on purpose: the Quartz job factory autowires BY TYPE, so a
    // def/Object-typed property is silently skipped and stays null.
    HazelcastInstance hazelcastInstance;
    MegaDriverService megaDriverService
    NotificationService notificationService

    // DISABLED: Grails auto-scheduling conflicts with SchedulerService
    // Jobs are now managed via SchedulerService and database-backed triggers
    /*
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
    */

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
        def expireMap = hazelcastInstance.getMap(CacheMap.EXPIRE.name)
        expireMap.entrySet().each { candidateForExpiration ->
            String portKey = String.valueOf(candidateForExpiration?.key)
            def objToExpire = candidateForExpiration?.value
            def now = DateTime.now()

            if (objToExpire?.peripheralId == null) {
                // An unusable entry would otherwise stick (and be skipped) forever.
                expireMap.remove(portKey)
                log.warn("Removed expiring-cache entry without peripheralId for port ${portKey}")
                return
            }
            if (!now.isAfter(objToExpire.expireOn)) {
                return
            }

            def port = DevicePort.get(portKey as Long)
            if (port == null || port.value == PortAction.OFF.name()) {
                // OFF confirmed by the device (or the port is gone) — deadline served.
                expireMap.remove(portKey)
                return
            }

            // The entry survives until an observed OFF removes it
            // (PortValueService), so a lost command retries at RETRY_DELAY_SEC
            // cadence instead of silently leaving the hardware ON.
            int attempts = (objToExpire.offAttempts ?: 0) as int
            publish(TopicName.EVT_LIGHT.id(), [
                    "p0": TopicName.EVT_LIGHT.id(),
                    "p1": EntityType.PERIPHERAL.name(),
                    "p2": objToExpire.peripheralId,
                    "p3": "timeout",
                    "p4": "off",
                    "p6": "system"
            ])
            if (attempts >= ESCALATE_AFTER_ATTEMPTS) {
                escalateUnconfirmedOff(port, attempts)
            }
            expireMap.put(portKey, [expireOn    : now.plusSeconds(RETRY_DELAY_SEC).toDate().time,
                                    peripheralId: objToExpire.peripheralId,
                                    offAttempts : attempts + 1])
        }
    }

    /**
     * MQTT retries alone did not produce a confirmed OFF: push the command over
     * the device's native HTTP interface and alert the admins — the hardware may
     * be physically stuck ON.
     */
    private void escalateUnconfirmedOff(DevicePort port, int attempts) {
        if (port.device?.model == DeviceModel.MEGAD_2561_RTC) {
            try {
                megaDriverService.writePortValues(port.device, [(port.internalRef): PortAction.OFF])
            } catch (Exception e) {
                log.error("Direct HTTP OFF failed for port ${port.id} (device ${port.device.code}): ${e.message}")
            }
        }
        try {
            notificationService.notifyAdmins(MessageLevel.ERROR,
                    "Auto-off unconfirmed: ${port.name ?: port.internalRef}",
                    "Port ${port.internalRef} of device ${port.device?.code} is still ON after ${attempts} switch-off attempts. " +
                            "The hardware may be stuck ON — check the device.",
                    'switch-off-timeout',
                    "autooff.${port.id}".toString(), 30)
        } catch (Exception e) {
            // A notification failure must never abort the switch-off sweep.
            log.error("Failed to send auto-off alert for port ${port.id}: ${e.message}")
        }
    }
}
