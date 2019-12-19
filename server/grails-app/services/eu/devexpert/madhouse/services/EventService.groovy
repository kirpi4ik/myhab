package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.infra.Zone
import eu.devexpert.madhouse.domain.job.EventData
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class EventService implements EventPublisher {

    def scenarioService
    def heatService

    @Subscriber('light')
    def recieveLightEvent(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findByUid(event.data.p2)
            def args = [:]
            args.portUids = []
            peripheral.getConnectedTo().each { port ->
                args.portUids << port.uid
            }
            switch (event.data.p4) {
                case "on":
                    scenarioService.lightsOn(args)
                    break
                case "off":
                    scenarioService.lightsOff(args)
                    break
                case "rev":
                    scenarioService.lightsReverse(args)
                    break
            }
        } else if (EntityType.ZONE.isEqual(event.data.p1)) {
            def zone = Zone.findByUid(event.data.p2)
            def eventArg = [:]
            def args = [:]
            args.portUids = []
            eventArg.portUids = fromZone(args.portUids, zone)


            switch (event.data.p4) {
                case "on":
                    scenarioService.lightsOn(eventArg)
                    break
                case "off":
                    scenarioService.lightsOff(eventArg)
                    break
                case "rev":
                    scenarioService.lightsReverse(eventArg)
                    break
            }
        }
    }

    @Subscriber('heat')
    def heat(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findByUid(event.data.p2)
            if (peripheral.category.uid == "7rer7ewh-f77a-451f-835e-ee21383503e5") {
                def args = [:]
                args.portUids = []
                peripheral.getConnectedTo().each { port ->
                    args.portUids << port.uid
                }
                switch (event.data.p4) {
                    case "on":
                        heatService.heatOn(peripheral)
                        break
                    case "off":
                        heatService.heatOff(peripheral)
                        break
                }
            }
        }
    }

    def fromZone(portUids, zone) {
        zone.zones.each {
            fromZone(portUids, it)
        }
        zone.getPeripherals().each { peripheral ->
            peripheral.getConnectedTo().each { port ->
                portUids << port.uid
            }
        }
        return portUids
    }

    @Subscriber('log_event')
    def logEvent(event) {
        EventData ev = event.data
        ev.save(failOnError: true, flush: true)
        log.debug(event.toString())
    }
}
