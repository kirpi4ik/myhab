package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.LockMode
import org.joda.time.DateTime

@Slf4j
@Transactional
class PortValueService implements EventPublisher {

    def updateIfChangedPortValues(deviceUid, portValues) {
        log.trace("Device[updateIfChangedPortValues] : "+deviceUid)
        portValues.each { portRef, actualDeviceValue ->
            try {
                def devicePorts = DevicePort.withCriteria {
                    eq('internalRef', portRef)
                    device { eq('uid', deviceUid) }
                    maxResults(1)
//                    delegate.criteria.lockMode = LockMode.UPGRADE_NOWAIT
                }
                if (devicePorts.size() > 0) {
                    updateIfChangedPortValue(devicePorts.first(), actualDeviceValue)
                }
            } catch (Exception ex) {
                log.error("Error reading port value : portRef=${portRef}, actualDeviceValue=${actualDeviceValue}", ex)
            }
        }
    }

    def updateIfChangedPortValue(DevicePort devicePort, String actualDeviceValue) {
        if (devicePort.type.syncMs != -1) {
            if (devicePort.value == null || DateTime.now().minus(devicePort.tsUpdated?.time).isAfter(devicePort?.type?.syncMs))
                if (!devicePort.value?.equalsIgnoreCase(ValueParser.parser(devicePort).apply(actualDeviceValue))) {
                    publish(TopicName.EVT_PORT_VALUE_CHANGED.id(), new EventData().with {
                        p0 = TopicName.EVT_PORT_VALUE_CHANGED.id()
                        p1 = EntityType.PORT.name()
                        p2 = "${devicePort.uid}"
                        p3 = "cron"
                        p4 = "$actualDeviceValue"
                        it
                    })
                }
        } else {
            //skip event
        }
    }
}
