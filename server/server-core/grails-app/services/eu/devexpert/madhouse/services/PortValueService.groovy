package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.hibernate.LockMode
import org.joda.time.DateTime

@Transactional
class PortValueService implements EventPublisher {

    def updateIfChangedPortValues(deviceUid, portValues) {
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
        def portLatestValues = PortValue.withCriteria {
            eq('portUid', devicePort.uid)
            gt("tsCreated", DateTime.now().minusDays(10).toDate())
            order("tsCreated", "desc")
            maxResults(1)
        }
        if (devicePort.type.syncMs != -1) {
            if (portLatestValues.size() == 0 || DateTime.now().minus(portLatestValues[0]?.tsCreated?.time).isAfter(devicePort?.type?.syncMs))
                if (devicePort.value == null || !devicePort.value.equalsIgnoreCase(ValueParser.parser(devicePort).apply(actualDeviceValue))) {
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
