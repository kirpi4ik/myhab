package eu.devexpert.madhouse.services.dsl.action

import eu.devexpert.madhouse.domain.device.port.DevicePort
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import eu.devexpert.madhouse.utils.http

@Slf4j
@Transactional
class PowerOut implements EventPublisher {

    PowerOut(params) {
        callAction(params)
    }

    @Transactional
    def callAction(params) {
        if (params != null) {
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
                http.get {
                    port = p
                    action = params.action
                }
                publish('light_is_on', p)

            }
        }
    }
}