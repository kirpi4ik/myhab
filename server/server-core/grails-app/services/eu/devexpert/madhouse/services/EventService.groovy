package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.DeviceStatus
import eu.devexpert.madhouse.domain.infra.Zone
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.jobs.PortValueReaderJob
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
class EventService implements EventPublisher {

    def scenarioService
    def heatService

    @Transactional
    @Subscriber('evt_light')
    def receiveLightEvent(event) {
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
        publish(TopicName.EVT_LOG.id(), event.data)
//        PortValueReaderJob.triggerNow();
    }

    @Transactional
    @Subscriber('evt_heat')
    def heat(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findByUid(event.data.p2)
            if (peripheral.category.name == "HEAT") {
                def ports = []
                peripheral.getConnectedTo().each { port ->
                    if (!port.value.equalsIgnoreCase(event.data.p4)) {
                        ports << port
                    }
                }
                if (ports.size() > 0) {
                    switch (event.data.p4.toLowerCase()) {
                        case "on":
                            heatService.heatOn(peripheral)
                            break
                        case "off":
                            heatService.heatOff(peripheral)
                            break
                    }
                    publish(TopicName.EVT_LOG.id(), event.data)
//                    PortValueReaderJob.triggerNow();
                }
            }
        }

    }

    @Transactional
    @Subscriber('evt_presence')
    def presence(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findByUid(event.data.p2)
            if (peripheral.category.name == "PRESENCE") {
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
        publish(TopicName.EVT_LOG.id(), event.data)
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

    @Transactional
    @Subscriber('evt_device_status')
    def deviceStatus(event) {
        if (EntityType.DEVICE.isEqual(event.data.p1)) {
            def device = Device.findByUid(event.data.p2)
            if (device.status != event.data.p4) {
                device.status = event.data.p4
                device.save(failOnError: true, flush: true)
            }
        }
    }

    @Transactional
    @Subscriber('evt_log')
    def logEvent(event) {
        EventData ev = event.data
        ev.save(failOnError: true, flush: true)
        log.debug(event.toString())
    }

}
