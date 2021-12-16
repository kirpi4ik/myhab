package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.async.mqtt.MqttTopicService
import eu.devexpert.madhouse.domain.device.Device
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

/**
 *
 */
@DisallowConcurrentExecution
@Transactional
class PortValueSyncTriggerJob implements Job, EventPublisher {

    MqttTopicService mqttTopicService


    static triggers = {
        simple name: 'portValueSyncTrigger', repeatInterval: TimeUnit.SECONDS.toMillis(60)
    }
    static group = "Internal"
    static description = "Trigger read port status"

    @Override
    @Transactional
    void execute(JobExecutionContext context) throws JobExecutionException {
        log.trace("PortValueReaderJob reader execute")
        Device.findAll().each { device ->
            def mqttSupported = device.getConfigurationByKey('cfg.key.device.mqtt.sync.supported')
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
