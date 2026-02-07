package org.myhab.services

import org.myhab.ConfigKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
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

    def powerService
    def heatService
    def intercomService

    /**
     * Handle light, switch, and heat events
     * All use the same logic: get ports and execute power action
     */
    @Transactional
    @Subscriber('evt_light')
    def receiveLightEvent(event) {
        handleSwitchEvent(event)
    }

    @Transactional
    @Subscriber('evt_switch')
    def receiveSwitchEvent(event) {
        handleSwitchEvent(event)
    }

    @Transactional
    @Subscriber('evt_heat')
    def receiveHeatEvent(event) {
        handleSwitchEvent(event)
    }

    /**
     * Common handler for switch/light/heat events
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
        
        if (portIds?.isEmpty() == false) {
            executePowerAction(action, portIds)
            publishEventLog(event.data)
        } else {
            log.warn("No port IDs found for entity type ${entityType} with ID ${event.data.p2}")
        }
    }

    /**
     * Get port IDs based on entity type
     * 
     * @param entityType The type of entity (PERIPHERAL, PORT, or ZONE)
     * @param entityId The ID of the entity
     * @return List of port IDs, or empty list if none found
     */
    private List<Long> getPortIdsForEntity(entityType, entityId) {
        switch (entityType) {
            case PERIPHERAL:
                def peripheral = DevicePeripheral.findById(entityId as Long)
                return peripheral?.getConnectedTo()?.collect { it.id } ?: []

            case PORT:
                return [entityId as Long]

            case ZONE:
                def zone = Zone.findById(entityId as Long)
                return zone ? collectPortIdsFromZone(zone) : []

            default:
                log.warn("Unsupported entity type: ${entityType}")
                return []
        }
    }

    /**
     * Execute power action based on command using PowerService
     * 
     * @param action The action string (on/off/rev)
     * @param portIds List of port IDs to execute the action on
     */
    private void executePowerAction(String action, List<Long> portIds) {
        PortAction portAction = mapActionToPortAction(action)
        
        if (!portAction) {
            log.warn("Unknown or unsupported action: ${action}")
            return
        }
        
        powerService.execute([portIds: portIds, action: portAction])
    }

    /**
     * Map action string to PortAction enum
     * 
     * @param action The action string (on/off/rev)
     * @return Corresponding PortAction or null if unknown
     */
    private PortAction mapActionToPortAction(String action) {
        switch (action?.toLowerCase()) {
            case "on":
                return PortAction.ON
            case "off":
                return PortAction.OFF
            case "rev":
            case "toggle":
                return PortAction.TOGGLE
            default:
                return null
        }
    }

    /**
     * Handle RGB color change events for lights
     * TODO: Remove hardcoded port IDs and make configurable
     */
    @Transactional
    @Subscriber('evt_light_set_color')
    def receiveColorEvent(event) {
        def peripheral = validatePeripheralEvent(event)
        if (!peripheral) return

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
     * Handle door lock/unlock events
     */
    @Transactional
    @Subscriber('evt_intercom_door_lock')
    def evt_intercom_door_lock(event) {
        def peripheral = validatePeripheralEvent(event)
        if (!peripheral) return

        if (!validatePeripheralCategory(peripheral, "DOOR_LOCK")) {
            return
        }

        def connectedPort = getFirstConnectedPort(peripheral)
        if (!connectedPort) return

        try {
            switch (event.data.p4?.toLowerCase()) {
                case "open":
                    intercomService.doorOpen(connectedPort.deviceId, connectedPort)
                    publishEventLog(event.data)
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
        def peripheral = validatePeripheralEvent(event)
        if (!peripheral) return

        if (!validatePeripheralCategory(peripheral, "PRESENCE")) {
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
            publishEventLog(event.data)
        } catch (Exception ex) {
            log.error("Error handling presence event for peripheral ${peripheral.id}", ex)
        }
    }

    /**
     * Collect all port IDs from a zone, including all peripherals in the zone 
     * and all child zones recursively.
     * 
     * <p>This method traverses the entire zone hierarchy starting from the given zone,
     * collecting port IDs from all peripherals found in each zone and sub-zone.</p>
     * 
     * @param zone The root zone to start collecting from
     * @return List of unique port IDs from all peripherals in the zone hierarchy
     */
    private List<Long> collectPortIdsFromZone(Zone zone) {
        if (!zone) {
            log.debug("Zone is null, returning empty port list")
            return []
        }

        List<Long> portIds = []
        collectPortIdsRecursively(zone, portIds)
        
        // Remove duplicates in case a port is referenced multiple times
        List<Long> uniquePortIds = portIds.unique()
        
        log.debug("Collected ${uniquePortIds.size()} unique port(s) from zone '${zone.name}' (id: ${zone.id}) and its children")
        return uniquePortIds
    }

    /**
     * Recursively collect port IDs from a zone and all its child zones
     * 
     * @param zone Current zone to process
     * @param portIds Accumulator list for port IDs
     */
    private void collectPortIdsRecursively(Zone zone, List<Long> portIds) {
        if (!zone) {
            return
        }

        // Collect port IDs from all peripherals directly in this zone
        zone.peripherals?.each { peripheral ->
            if (peripheral) {
                def connectedPorts = peripheral.connectedTo
                if (connectedPorts) {
                    connectedPorts.each { port ->
                        if (port?.id) {
                            portIds << port.id
                            log.trace("Added port ${port.id} from peripheral '${peripheral.name}' in zone '${zone.name}'")
                        }
                    }
                } else {
                    log.trace("Peripheral '${peripheral.name}' (id: ${peripheral.id}) in zone '${zone.name}' has no connected ports")
                }
            }
        }

        // Recursively process all child zones
        zone.zones?.each { childZone ->
            if (childZone) {
                log.trace("Processing child zone '${childZone.name}' (id: ${childZone.id}) of parent '${zone.name}'")
                collectPortIdsRecursively(childZone, portIds)
            }
        }
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
            if (validatePeripheralCategory(peripheral, "ACCESS_CONTROL", false)) {
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
     * Check if access token is valid (matches token value and not expired)
     * 
     * @param accessToken The access token to validate
     * @param tokenValue The token value to check against
     * @return true if token is valid and not expired, false otherwise
     */
    private boolean isValidAccessToken(accessToken, tokenValue) {
        if (!accessToken || accessToken.token != tokenValue) {
            return false
        }

        // Check expiration - null means no expiration
        return !accessToken.tsExpiration || accessToken.tsExpiration.after(DateTime.now().toDate())
    }

    /**
     * Publish door unlock event for access control
     * 
     * @param peripheral The peripheral being unlocked
     * @param unlockCode The access code used for unlocking
     * @param userName Name of the user who unlocked
     */
    private void publishDoorUnlockEvent(DevicePeripheral peripheral, String unlockCode, String userName) {
        try {
            def eventData = new EventData().with {
                p0 = TopicName.EVT_INTERCOM_DOOR_LOCK.id()
                p1 = PERIPHERAL.name()
                p2 = "${peripheral.id}"
                p3 = "access control"
                p4 = "open"
                p5 = "{\"unlockCode\": \"${unlockCode}\"}"
                p6 = userName
                it
            }
            publish(TopicName.EVT_INTERCOM_DOOR_LOCK.id(), eventData)
            log.debug("Published door unlock event for peripheral ${peripheral.id}, user: ${userName}")
        } catch (Exception ex) {
            log.error("Failed to publish door unlock event for peripheral ${peripheral?.id}", ex)
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Validate peripheral event and retrieve peripheral
     * 
     * @param event Event data containing entity type and peripheral ID
     * @return DevicePeripheral instance or null if validation fails
     */
    private DevicePeripheral validatePeripheralEvent(event) {
        if (!PERIPHERAL.isEqual(event.data?.p1)) {
            log.debug("Event is not a peripheral event, p1: ${event.data?.p1}")
            return null
        }

        if (!event.data.p2) {
            log.warn("Peripheral ID (p2) is missing in event data")
            return null
        }

        def peripheral = DevicePeripheral.findById(event.data.p2 as Long)
        if (!peripheral) {
            log.warn("Peripheral not found with ID: ${event.data.p2}")
        }
        return peripheral
    }

    /**
     * Validate peripheral category matches expected category
     * 
     * @param peripheral The peripheral to validate
     * @param expectedCategory Expected category name (e.g., "DOOR_LOCK", "PRESENCE")
     * @param logMismatch Whether to log when category doesn't match (default: true)
     * @return true if category matches, false otherwise
     */
    private boolean validatePeripheralCategory(DevicePeripheral peripheral, String expectedCategory, boolean logMismatch = true) {
        boolean matches = peripheral?.category?.name == expectedCategory
        
        if (!matches && logMismatch) {
            log.debug("Peripheral ${peripheral?.id} category mismatch. Expected: ${expectedCategory}, Actual: ${peripheral?.category?.name}")
        }
        
        return matches
    }

    /**
     * Get first connected port from peripheral with null safety
     * 
     * @param peripheral The peripheral to get connected port from
     * @return First connected DevicePort or null if none found
     */
    private DevicePort getFirstConnectedPort(DevicePeripheral peripheral) {
        def connectedPort = peripheral?.connectedTo?.find()
        
        if (!connectedPort) {
            log.warn("No connected port found for peripheral ${peripheral?.id}")
        }
        
        return connectedPort
    }

    /**
     * Publish event to event log
     * @param eventData Event data to publish (can be EventData or Map)
     */
    private void publishEventLog(def eventData) {
        publish(TopicName.EVT_LOG.id(), eventData)
    }

}
