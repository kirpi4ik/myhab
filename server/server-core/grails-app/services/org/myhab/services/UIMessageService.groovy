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

    /**
     * Handle light switch events
     */
    @Transactional
    @Subscriber('evt_light')
    def receiveLightEvent(event) {
        handleSwitchEvent(event)
    }

    /**
     * Handle generic switch events
     */
    @Transactional
    @Subscriber('evt_switch')
    def receiveSwitchEvent(event) {
        handleSwitchEvent(event)
    }

    /**
     * Common handler for switch/light events
     * Handles PERIPHERAL, PORT, and ZONE entity types
     */
    private void handleSwitchEvent(event) {
        def entityType = byName(event.data.p1)
        def action = event.data.p4

        if (!action) {
            log.warn("No action specified in event: ${event.data}")
            return
        }

        def portIds = getPortIdsForEntity(entityType, event.data.p2)
        if (portIds) {
            executeScenarioAction(action, [portIds: portIds])
            publish(TopicName.EVT_LOG.id(), event.data)
        } else {
            log.warn("No port IDs found for entity type ${entityType} with ID ${event.data.p2}")
        }
    }

    /**
     * Get port IDs based on entity type
     */
    private List<Long> getPortIdsForEntity(entityType, entityId) {
        switch (entityType) {
            case PERIPHERAL:
                def peripheral = DevicePeripheral.findById(entityId as Integer)
                return peripheral?.getConnectedTo()?.collect { it.id } ?: []

            case PORT:
                return [entityId]

            case ZONE:
                def zone = Zone.findById(entityId)
                return zone ? fromZone([], zone) : []

            default:
                log.warn("Unsupported entity type: ${entityType}")
                return []
        }
    }

    /**
     * Execute scenario action based on command
     */
    private void executeScenarioAction(String action, Map args) {
        switch (action?.toLowerCase()) {
            case "on":
                scenarioService.switchOn(args)
                break
            case "off":
                scenarioService.switchOff(args)
                break
            case "rev":
                scenarioService.switchToggle(args)
                break
            default:
                log.warn("Unknown action: ${action}")
        }
    }

    /**
     * Handle RGB color change events for lights
     * TODO: Remove hardcoded port IDs and make configurable
     */
    @Transactional
    @Subscriber('evt_light_set_color')
    def receiveColorEvent(event) {
        if (!PERIPHERAL.isEqual(event.data.p1)) {
            return
        }

        def peripheral = DevicePeripheral.findById(event.data.p2)
        if (!peripheral) {
            log.warn("Peripheral not found with ID: ${event.data.p2}")
            return
        }

        try {
            def color = new JsonSlurper().parseText(event.data.p4)
            
            // TODO: Make RGB port IDs configurable via peripheral configuration
            def rPort = DevicePort.findById(22633)
            def gPort = DevicePort.findById(22634)
            def bPort = DevicePort.findById(22632)

            if (!rPort || !gPort || !bPort) {
                log.error("RGB ports not found for peripheral ${peripheral.id}")
                return
            }

            new DeviceHttpService(port: rPort, action: color.rgb.r).writeState()
            new DeviceHttpService(port: gPort, action: color.rgb.g).writeState()
            new DeviceHttpService(port: bPort, action: color.rgb.b).writeState()

            log.debug("RGB color set successfully for peripheral ${peripheral.id}")
        } catch (Exception ex) {
            log.error("Failed to set RGB color for peripheral ${event.data.p2}", ex)
        }
    }

    /**
     * Handle heat control events
     */
    @Transactional
    @Subscriber('evt_heat')
    def heat(event) {
        if (!PERIPHERAL.isEqual(event.data.p1)) {
            return
        }

        def peripheral = DevicePeripheral.findById(event.data.p2)
        if (!peripheral) {
            log.warn("Peripheral not found with ID: ${event.data.p2}")
            return
        }

        if (peripheral.category?.name != "HEAT") {
            log.debug("Ignoring heat event for non-heat peripheral: ${peripheral.id}")
            return
        }

        // Check if any port needs to change state
        def portsToChange = peripheral.getConnectedTo()?.findAll { port ->
            !port.value.equalsIgnoreCase(event.data.p4)
        }

        if (!portsToChange) {
            log.debug("No state change needed for peripheral ${peripheral.id}")
            return
        }

        try {
            switch (event.data.p4?.toLowerCase()) {
                case "on":
                    heatService.heatOn(peripheral)
                    break
                case "off":
                    heatService.heatOff(peripheral)
                    break
                default:
                    log.warn("Unknown heat action: ${event.data.p4}")
                    return
            }
            publish(TopicName.EVT_LOG.id(), event.data)
        } catch (Exception ex) {
            log.error("Error handling heat event for peripheral ${peripheral.id}", ex)
        }
    }

    /**
     * Handle door lock/unlock events
     */
    @Transactional
    @Subscriber('evt_intercom_door_lock')
    def evt_intercom_door_lock(event) {
        if (!PERIPHERAL.isEqual(event.data.p1)) {
            return
        }

        def peripheral = DevicePeripheral.findById(event.data.p2)
        if (!peripheral) {
            log.warn("Peripheral not found with ID: ${event.data.p2}")
            return
        }

        if (peripheral.category?.name != "DOOR_LOCK") {
            log.debug("Ignoring door lock event for non-door-lock peripheral: ${peripheral.id}")
            return
        }

        def connectedPort = peripheral.getConnectedTo()?.first()
        if (!connectedPort) {
            log.warn("No connected port found for door lock peripheral ${peripheral.id}")
            return
        }

        try {
            switch (event.data.p4?.toLowerCase()) {
                case "open":
                    intercomService.doorOpen(connectedPort.deviceId, connectedPort)
                    publish(TopicName.EVT_LOG.id(), event.data)
                    break
                default:
                    log.warn("Unknown door lock action: ${event.data.p4}")
            }
        } catch (Exception ex) {
            log.error("Error handling door lock event for peripheral ${peripheral.id}", ex)
        }
    }

    /**
     * Handle presence sensor events
     * Currently disabled - uncomment @Subscriber annotation to enable
     */
    @Transactional
//    @Subscriber('evt_presence')
    def presence(event) {
        if (!PERIPHERAL.isEqual(event.data.p1)) {
            return
        }

        def peripheral = DevicePeripheral.findById(event.data.p2 as Long)
        if (!peripheral) {
            log.warn("Peripheral not found with ID: ${event.data.p2}")
            return
        }

        if (peripheral.category?.name != "PRESENCE") {
            return
        }

        try {
            switch (event.data.p4?.toLowerCase()) {
                case "on":
                    heatService.heatOn(peripheral)
                    break
                case "off":
                    heatService.heatOff(peripheral)
                    break
                default:
                    log.warn("Unknown presence action: ${event.data.p4}")
            }
            publish(TopicName.EVT_LOG.id(), event.data)
        } catch (Exception ex) {
            log.error("Error handling presence event for peripheral ${peripheral.id}", ex)
        }
    }

    /**
     * Recursively collect port IDs from a zone and its sub-zones
     * @param portIds List to collect port IDs into
     * @param zone Zone to extract port IDs from
     * @return List of port IDs
     */
    private List<Long> fromZone(List<Long> portIds, Zone zone) {
        if (!zone) {
            return portIds
        }

        // Process sub-zones recursively
        zone.zones?.each { subZone ->
            fromZone(portIds, subZone)
        }

        // Collect port IDs from peripherals in this zone
        zone.getPeripherals()?.each { peripheral ->
            peripheral.getConnectedTo()?.each { port ->
                portIds << port.id
            }
        }

        return portIds
    }

    /**
     * Log events to database
     */
    @Transactional
    @Subscriber('evt_log')
    def logEvent(event) {
        try {
            EventData ev = event.data
            ev.save(failOnError: true, flush: true)
            log.debug("Event logged: ${event}")
        } catch (Exception ex) {
            log.error("Failed to log event: ${event.data}", ex)
        }
    }

    /**
     * @deprecated This method is no longer needed since uid is deprecated. Use id instead.
     */
    @Deprecated
    def isUid(id) {
        return !id.matches("[0-9]+")
    }

    /**
     * Handle events pushed from devices (e.g., access control tokens)
     */
    @Transactional
    @Subscriber('evt_device_push')
    def receiveEventFromDevice(event) {
        if (!event.data?.mdid || !event.data?.pt) {
            log.warn("Missing device ID (mdid) or port reference (pt) in device push event")
            return
        }

        Device device = Device.findByCode(event.data.mdid)
        if (!device) {
            log.warn("Device not found with code: ${event.data.mdid}")
            return
        }

        DevicePort port = DevicePort.where {
            internalRef == event.data.pt
            device.id == device.id
        }.first()

        if (!port) {
            log.warn("Port not found with reference: ${event.data.pt} for device: ${device.code}")
            return
        }

        port.peripherals?.each { peripheral ->
            if (peripheral.category?.name == "ACCESS_CONTROL") {
                handleAccessControlEvent(peripheral, event.data)
            }
        }
    }

    /**
     * Handle access control peripheral events
     */
    private void handleAccessControlEvent(DevicePeripheral peripheral, eventData) {
        def relatedPeripheralConfigs = peripheral.getConfigurationByKey(
            ConfigKey.CONFIG_LIST_RELATED_PERIPHERAL_IDS
        )

        relatedPeripheralConfigs?.each { config ->
            def accessPeripheral = DevicePeripheral.findById(config.value)
            if (!accessPeripheral) {
                log.warn("Related peripheral not found with ID: ${config.value}")
                return
            }

            accessPeripheral.accessTokens?.each { accessToken ->
                if (isValidAccessToken(accessToken, eventData.wg)) {
                    publishDoorUnlockEvent(accessPeripheral, eventData.wg, accessToken.user.name)
                } else {
                    log.error("Invalid or expired access token: ${eventData.wg}")
                }
            }
        }
    }

    /**
     * Check if access token is valid
     */
    private boolean isValidAccessToken(accessToken, tokenValue) {
        if (accessToken.token != tokenValue) {
            return false
        }

        // Check expiration
        return accessToken.tsExpiration == null || 
               accessToken.tsExpiration.after(DateTime.now().toDate())
    }

    /**
     * Publish door unlock event
     */
    private void publishDoorUnlockEvent(DevicePeripheral peripheral, String unlockCode, String userName) {
        try {
            publish(TopicName.EVT_INTERCOM_DOOR_LOCK.id(), new EventData().with {
                p0 = TopicName.EVT_INTERCOM_DOOR_LOCK.id()
                p1 = PERIPHERAL.name()
                p2 = peripheral.id
                p3 = "access control"
                p4 = "open"
                p5 = '{"unlockCode": "' + unlockCode + '"}'
                p6 = userName
                it
            })
        } catch (Exception ex) {
            log.error("Failed to publish door unlock event for peripheral ${peripheral.id}", ex)
        }
    }

}
