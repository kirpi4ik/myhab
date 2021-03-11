package eu.devexpert.madhouse.controller

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.job.EventData
import grails.events.EventPublisher

class EventController implements EventPublisher {
    static responseFormats = ['json', 'xml']

    def pubGetEvent() {
        EventData input = new EventData(params)

        if (input.p0 && request.remoteAddr.startsWith("192.")) {
            publish(input.p0, input)
            publish(TopicName.EVT_LOG.id(), input)
        } else {
            log.info("Event triggered with:  $params")
            respond params
        }
    }

    def shortUrlEvent() {
        def input = params.p0.split(":")
        def mega = Device.findByCode(input[0])

        def devicePort = DevicePort.withCriteria {
            eq('internalRef', input[1])
            device {
                eq('uid', mega.uid)
            }
            maxResults(1)
        }
        EventData eObj = new EventData().with {
            p0 = TopicName.byOrder(input[2] as Integer).id()
            p1 = EntityType.PORT.name()
            p2 = "${devicePort.uid}"
            p3 = "sensor-${input[0]}-${input[1]}"
            p4 = "ON"
            it
        }
        publish(eObj.p0, eObj)
        publish(TopicName.EVT_LOG.id(), eObj)
    }
}
