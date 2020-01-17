package eu.devexpert.madhouse.listener

import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.transaction.annotation.Propagation

@Transactional
@Slf4j
class PortValueChangeListenerService {

    @Subscriber("port_value_change")
    @Transactional(propagation = Propagation.REQUIRED)
    def port_value_change(event) {
        def port = DevicePort.findByUid(event.data.p2)
        def newVal = ValueParser.parser(port).apply(event.data.p4)

        log.debug("Port [id: ${port.id}, name: ${port.name}] value changed to ${newVal}")

        PortValue newPortValue = new PortValue()
        newPortValue.portUid = port.uid
        newPortValue.value = newVal
        newPortValue.save(failOnError: true, flush: true)

        port.setValue(newVal)
        port.save(failOnError: true, flush: true)

    }
}
