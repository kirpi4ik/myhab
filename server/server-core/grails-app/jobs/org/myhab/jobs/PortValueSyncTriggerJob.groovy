package org.myhab.jobs

import grails.events.EventPublisher
import grails.util.Holders
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

/**
 *
 */
@DisallowConcurrentExecution
class PortValueSyncTriggerJob implements Job, EventPublisher {

    MqttTopicService mqttTopicService


    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.portValueSyncTrigger.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.portValueSyncTrigger.interval', Integer) ?: 60
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            println "PortValueSyncTriggerJob: ENABLED - Registering trigger with interval ${interval}s"
            simple name: 'portValueSyncTrigger', repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            println "PortValueSyncTriggerJob: DISABLED - Not registering trigger"
        }
    }
    static group = "Internal"
    static description = "Trigger read port status"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.portValueSyncTrigger.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("PortValueSyncTriggerJob is DISABLED via configuration, skipping execution")
            return
        }
        log.trace("PortValueReaderJob reader execute")
        Device.findAll().each { device ->
            def mqttSupported = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_MQTT_SYNC_SUPPORTED)
            if (mqttSupported && Boolean.valueOf(mqttSupported.value)) {
                device.ports.each { port ->
                    try {
                        mqttTopicService.forceRead(port, ["get:${port.internalRef}"])
                    } catch (Exception ex) {
                        log.warn(ex.message)
                    }
                }
            }
        }
    }


}
