package eu.devexpert.madhouse.listener.events


import eu.devexpert.madhouse.ConfigKey
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.init.cache.CacheMap
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Propagation

@Transactional
@Slf4j
class TwoStatePortValueChangeListenerService {
    def hazelcastInstance;


    @Subscriber("evt_port_value_changed")
    @Transactional(propagation = Propagation.REQUIRED)
    def update_port_value(event) {
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

    @Subscriber("evt_port_value_changed")
    @Transactional(propagation = Propagation.REQUIRED)
    def update_expiration_time(event) {
        def port = DevicePort.findByUid(event.data.p2)
        def newVal = ValueParser.parser(port).apply(event.data.p4)

        def peripheral = port.peripherals[0]
        if (peripheral != null) {
            def config = peripheral.configurations.find { it.key == ConfigKey.STATE_ON_TIMEOUT }
            if (config != null && newVal == PortAction.ON.name()) {
                def expireInMs = DateTime.now().plusSeconds(Integer.valueOf(config.value)).toDate().time
                hazelcastInstance.getMap(CacheMap.EXPIRE).put(String.valueOf(port.id), [expireOn: expireInMs, portUid: peripheral.uid])
            } else if (newVal == PortAction.OFF.name()) {
                hazelcastInstance.getMap(CacheMap.EXPIRE).remove(String.valueOf(port.id))
            }
        }
    }
}
