package org.myhab.services.dsl.action


import org.myhab.domain.device.port.DevicePort
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
class PowerService implements EventPublisher {

    def mqttTopicService

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
                log.debug("power switch : ${p} - ${params.action}")
                mqttTopicService.publish(p, [params.action])
            }
        }
    }
}