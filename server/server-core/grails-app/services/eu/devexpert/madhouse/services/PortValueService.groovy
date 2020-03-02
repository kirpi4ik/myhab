package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.parser.ValueParser
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.joda.time.DateTime

@Transactional
class PortValueService implements EventPublisher {

    def updatePortValues(deviceUid, portValues) {
        portValues.each { portRef, newValue ->
            try {
                def devicePorts = DevicePort.withCriteria {
                    eq('internalRef', portRef)
                    device {
                        eq('uid', deviceUid)
                    }
                    maxResults(1)
                }
                if (devicePorts.size() > 0) {
                    updatePortValue(devicePorts[0], newValue)
                }
            } catch (Exception ex) {
                log.error("Error reading port value : portRef=${portRef}, newValue=${newValue}", ex)
            }
        }
    }

    def updatePortValueByRefId(String internalRefId, String newValue) {

    }

    def updatePortValueByUid(String devicePortUid, String newValue) {

    }

    def updatePortValue(DevicePort devicePort, String newValue) {
        def portLatestValues = PortValue.withCriteria {
            eq('portUid', devicePort.uid)
            order("tsCreated", "desc")
            maxResults(1)
        }
        if (devicePort.type.syncMs != -1) {
            if (portLatestValues.size() == 0 || DateTime.now().minus(portLatestValues[0]?.tsCreated?.time).isAfter(devicePort?.type?.syncMs))
                if (devicePort.value == null || !devicePort.value.equalsIgnoreCase(ValueParser.parser(devicePort).apply(newValue))) {
                    publish(TopicName.EVT_PORT_VALUE_CHANGED.id(), new EventData().with {
                        p0 = TopicName.EVT_PORT_VALUE_CHANGED.id()
                        p1 = "PORT"
                        p2 = "${devicePort.uid}"
                        p3 = "cron"
                        p4 = "$newValue"
                        it
                    })
                }
        } else {
            //skip event
        }
    }
}
