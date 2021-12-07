package eu.devexpert.madhouse.services.dsl.action

import eu.devexpert.madhouse.async.mqtt.MQTTMessage
import eu.devexpert.madhouse.domain.device.port.DevicePort
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class PowerService implements EventPublisher {

    def mqttTopicService

    @Transactional
    def execute(params) {
        if (params != null) {
            Collections.emptyList()
            def ports = []
            if (params.portUids != null) {
                params.portUids.each { uid ->
                    ports << DevicePort.findByUid(uid)
                }
            }
            if (params.portIds != null) {
                params.portIds.each { id ->
                    ports << DevicePort.findById(Long.valueOf(id))
                }
            }

            ports.each { p ->
                log.debug("light on ${p}")
                mqttTopicService.publish(p, [params.action])
            }
        }
    }
}