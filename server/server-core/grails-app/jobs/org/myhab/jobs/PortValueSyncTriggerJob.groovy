package org.myhab.jobs

import grails.events.EventPublisher
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
        simple name: 'portValueSyncTrigger', repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }
    static group = "Internal"
    static description = "Trigger read port status"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
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
