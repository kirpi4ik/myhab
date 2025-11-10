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
            def ports = []
            
            // Use portIds (id-based lookups)
            if (params.portIds != null) {
                params.portIds.each { id ->
                    def port = DevicePort.findById(Long.valueOf(id))
                    if (port) {
                        ports << port
                    } else {
                        log.warn("DevicePort not found for id: ${id}")
                    }
                }
            }

            // Execute action on all found ports
            ports.each { port ->
                log.debug("Power switch: ${port.name} (id: ${port.id}) - action: ${params.action}")
                mqttTopicService.publish(port, [params.action])
            }
            
            if (ports.isEmpty()) {
                log.warn("No ports found to execute action: ${params.action}")
            }
        }
    }
}
