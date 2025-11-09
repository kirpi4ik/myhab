package org.myhab.services

import org.myhab.ConfigKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.EventData
import org.myhab.utils.DeviceHttpService
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.joda.time.DateTime

import static org.myhab.domain.EntityType.*

@Slf4j
class UIMessageService implements EventPublisher {

    def scenarioService
    def heatService
    def intercomService

    @Transactional
    @Subscriber('evt_light')
    def receiveLightEvent(event) {
        switch (byName(event.data.p1)) {
            case PERIPHERAL:
                def peripheral = DevicePeripheral.findById(event.data.p2 as Integer)
                def args = [:]
                args.portIds = []
                peripheral?.getConnectedTo()?.each { port ->
                    args.portIds << port.id
                }
                switch (event.data.p4) {
                    case "on":
                        scenarioService.switchOn(args)
                        break
                    case "off":
                        scenarioService.switchOff(args)
                        break
                    case "rev":
                        scenarioService.switchToggle(args)
                        break
                }
                break
            case PORT:
                def args = [:]
                args.portIds = [event.data.p2]

                switch (event.data.p4) {
                    case "on":
                        scenarioService.switchOn(args)
                        break
                    case "off":
                        scenarioService.switchOff(args)
                        break
                    case "rev":
                        scenarioService.switchToggle(args)
                        break
                }
                break
            case ZONE:
                def zone = Zone.findById(event.data.p2)
                def eventArg = [:]
                def args = [:]
                args.portIds = []
                eventArg.portIds = fromZone(args.portIds, zone)


                switch (event.data.p4) {
                    case "on":
                        scenarioService.switchOn(eventArg)
                        break
                    case "off":
                        scenarioService.switchOff(eventArg)
                        break
                    case "rev":
                        scenarioService.switchToggle(eventArg)
                        break
                }
                break

        }

        publish(TopicName.EVT_LOG.id(), event.data)
//        PortValueReaderJob.triggerNow();
    }

    @Transactional
    @Subscriber('evt_light_set_color')
    def receiveColorEvent(event) {
        if (PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findById(event.data.p2)
            def args = [:]
            args.portIds = []
            peripheral.getConnectedTo().each { port ->
                args.portIds << port.id
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
                log.error("Failed to set RGB color", ex)
            }
        }
    }

    @Transactional
    @Subscriber('evt_heat')
    def heat(event) {
        if (PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findById(event.data.p2)
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
        if (PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findById(event.data.p2)
            if (peripheral.category.name == "DOOR_LOCK") {
                switch (event.data.p4.toLowerCase()) {
                    case "open":
                        intercomService.doorOpen(peripheral.getConnectedTo().first()?.deviceId, peripheral.getConnectedTo().first())
                        break
                }
                publish(TopicName.EVT_LOG.id(), event.data)
//                    PortValueReaderJob.triggerNow();

            }
        }

    }

    @Transactional
//    @Subscriber('evt_presence')
    def presence(event) {
        if (PERIPHERAL.isEqual(event.data.p1)) {
            def peripheral = DevicePeripheral.findById(Long.valueOf(event.data.p2))
            if (peripheral.category.name == "PRESENCE") {
                def args = [:]
                args.portIds = []
                peripheral.getConnectedTo().each { port ->
                    args.portIds << port.id
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

    def fromZone(portIds, zone) {
        zone.zones.each {
            fromZone(portIds, it)
        }
        zone.getPeripherals().each { peripheral ->
            peripheral.getConnectedTo().each { port ->
                portIds << port.id
            }
        }
        return portIds
    }

    @Transactional
    @Subscriber('evt_log')
    def logEvent(event) {
        EventData ev = event.data
        ev.save(failOnError: true, flush: true)
        log.debug(event.toString())
    }

    /**
     * @deprecated This method is no longer needed since uid is deprecated. Use id instead.
     */
    @Deprecated
    def isUid(id) {
        return !id.matches("[0-9]+")
    }

    @Transactional
    @Subscriber('evt_device_push')
    def receiveEventFromDevice(event) {
        if (event.data.mdid && event.data.pt) {
            Device dvc = Device.findByCode(event.data.mdid)
            DevicePort port = DevicePort.where {
                internalRef == event.data.pt
                device.id == dvc.id
            }.first()
            port.peripherals.each { peripheral ->
                if (peripheral.category.name == "ACCESS_CONTROL") {
                    peripheral.getConfigurationByKey(ConfigKey.CONFIG_LIST_RELATED_PERIPHERAL_IDS).each { config ->
                        def accPeripheral = DevicePeripheral.findById(config.value)
                        accPeripheral?.accessTokens?.each { accToken ->
                            if (accToken.token == event.data.wg && (accToken.tsExpiration == null || accToken.tsExpiration.after(DateTime.now().toDate()))) {
                                publish(TopicName.EVT_INTERCOM_DOOR_LOCK.id(), new EventData().with {
                                    p0 = TopicName.EVT_INTERCOM_DOOR_LOCK.id()
                                    p1 = PERIPHERAL.name()
                                    p2 = accPeripheral.id
                                    p3 = "access control"
                                    p4 = "open"
                                    p5 = '{"unlockCode": "' + event.data.wg + '"}'
                                    p6 = accToken.user.name
                                    it
                                })
                            } else {
                                log.error("Invalid access token ${event.data.wg}")
                            }
                        }
                    }
                }
            }
        }
    }

}
