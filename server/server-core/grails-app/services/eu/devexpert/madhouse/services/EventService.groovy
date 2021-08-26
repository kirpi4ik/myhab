package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.DeviceStatus
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.infra.Zone
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.jobs.PortValueReaderJob
import eu.devexpert.madhouse.utils.DeviceHttpService
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class EventService implements EventPublisher {

    def scenarioService
    def heatService
    def intercomService

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
    @Subscriber('evt_light_set_color')
    def receiveColorEvent(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findByUid(event.data.p2)
            def args = [:]
            args.portUids = []
            peripheral.getConnectedTo().each { port ->
                args.portUids << port.uid
            }
            try {
                def color = new JsonSlurper().parseText(event.data.p4)
                def b = DevicePort.findById(22632)
                def r = DevicePort.findById(22633)
                def g = DevicePort.findById(22634)
                new DeviceHttpService(port: r, action: color.rgb.r).writeState()
                new DeviceHttpService(port: g, action: color.rgb.g).writeState()
                new DeviceHttpService(port: b, action: color.rgb.b).writeState()

            } catch (Exception ex) {
                ex.printStackTrace()
            }
        }
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
    @Subscriber('evt_intercom_door_lock')
    def evt_intercom_door_lock(event) {
        if (EntityType.PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findById(event.data.p2)
            if (peripheral.category.name == "DOOR_LOCK") {
                switch (event.data.p4.toLowerCase()) {
                    case "open":
                        intercomService.doorOpen(peripheral.getConnectedTo().first()?.deviceId)
                        break
                }
                publish(TopicName.EVT_LOG.id(), event.data)
//                    PortValueReaderJob.triggerNow();

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

    /**
     * Update device status : OFFLINE/ONLINE
     * @param event
     * @return
     */
    @Transactional
    @Subscriber('evt_device_status')
    def deviceStatus(event) {
        if (EntityType.DEVICE.isEqual(event.data.p1)) {
            def device
            if (isUid(event.data.p2)) {
                device = Device.findByUid(event.data.p2)
            } else {
                device = Device.findById(event.data.p2)
            }
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

    def isUid(id) {
        return !id.matches("[0-9]+")
    }
}
