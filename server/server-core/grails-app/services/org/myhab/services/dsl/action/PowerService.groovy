package org.myhab.services.dsl.action


import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.device.DevicePeripheral
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

/**
 * PowerService handles power control operations for device ports in the MyHAB system.
 * 
 * <p>This service is responsible for executing power-related actions (ON, OFF, TOGGLE) 
 * on device ports by publishing MQTT commands. It supports batch operations on multiple 
 * ports simultaneously.</p>
 * 
 * <p>The service integrates with:</p>
 * <ul>
 *   <li><b>DevicePort domain</b> - Database access to port entities</li>
 *   <li><b>MqttTopicService</b> - MQTT message publishing</li>
 *   <li><b>ScenarioService</b> - Called via DSL methods (switchOn, switchOff, switchToggle)</li>
 * </ul>
 * 
 * <p>Example usage from DSL:</p>
 * <pre>
 * // Turn on multiple ports by ID
 * switchOn([portIds: [123, 456]])
 * 
 * // Turn on ports via peripheral IDs
 * switchOn([peripheralIds: [10, 11, 12]])
 * 
 * // Combine both approaches
 * switchOn([portIds: [123], peripheralIds: [10, 11]])
 * 
 * // Toggle a single port
 * switchToggle([portIds: [789]])
 * </pre>
 * 
 * @author MyHAB Team
 * @since 1.0
 * @see org.myhab.services.dsl.ScenarioService
 */
@Slf4j
@Transactional(readOnly = true)
class PowerService implements EventPublisher {

    def mqttTopicService

    /**
     * Execute a power action on the specified device ports.
     * 
     * <p>This method supports two ways to specify target ports:</p>
     * <ol>
     *   <li><b>Direct port selection</b> via portIds - looks up ports by their database IDs</li>
     *   <li><b>Peripheral-based selection</b> via peripheralIds - finds peripherals and uses their connectedTo ports</li>
     * </ol>
     * 
     * <p>Both approaches can be combined in a single call. The operation is optimized 
     * to fetch all entities in batch queries and automatically removes duplicate ports.</p>
     * 
     * <p>If any port or peripheral IDs are not found in the database, a warning is logged but 
     * the operation continues for the remaining valid entities.</p>
     * 
     * <p>Supported actions:</p>
     * <ul>
     *   <li>{@link PortAction#ON} - Turn port ON</li>
     *   <li>{@link PortAction#OFF} - Turn port OFF</li>
     *   <li>{@link PortAction#TOGGLE} - Toggle port state</li>
     * </ul>
     * 
     * @param params Map containing execution parameters:
     *               <ul>
     *                 <li><b>portIds</b> (List&lt;Long&gt;) - Optional list of device port IDs to control</li>
     *                 <li><b>peripheralIds</b> (List&lt;Long&gt;) - Optional list of peripheral IDs (uses their connected ports)</li>
     *                 <li><b>action</b> (PortAction) - Action to execute (ON, OFF, TOGGLE)</li>
     *               </ul>
     *               At least one of portIds or peripheralIds must be provided.
     * @throws IllegalArgumentException if params is missing required fields
     * @throws NumberFormatException if portIds or peripheralIds contain invalid numeric values
     */
    def execute(Map params) {
        // Validate input parameters
        if (params == null) {
            log.warn("PowerService.execute called with null params")
            return
        }
        
        if (params.action == null) {
            log.error("PowerService.execute called without action parameter")
            return
        }
        
        boolean hasPortIds = params.portIds != null && !params.portIds.isEmpty()
        boolean hasPeripheralIds = params.peripheralIds != null && !params.peripheralIds.isEmpty()
        
        if (!hasPortIds && !hasPeripheralIds) {
            log.warn("PowerService.execute called with no portIds or peripheralIds for action: ${params.action}")
            return
        }
        
        try {
            List<DevicePort> ports = []
            
            // Process portIds if provided
            if (hasPortIds) {
                List<Long> portIds = params.portIds.collect { Long.valueOf(it) }
                log.debug("Fetching ${portIds.size()} port(s) by ID: ${portIds}")
                
                List<DevicePort> directPorts = DevicePort.findAllByIdInList(portIds)
                
                if (directPorts.size() < portIds.size()) {
                    def foundIds = directPorts*.id
                    def notFoundIds = portIds - foundIds
                    log.warn("DevicePort(s) not found for id(s): ${notFoundIds}")
                }
                
                ports.addAll(directPorts)
                log.debug("Found ${directPorts.size()} port(s) from portIds")
            }
            
            // Process peripheralIds if provided
            if (hasPeripheralIds) {
                List<Long> peripheralIds = params.peripheralIds.collect { Long.valueOf(it) }
                log.debug("Fetching ${peripheralIds.size()} peripheral(s) by ID: ${peripheralIds}")
                
                List<DevicePeripheral> peripherals = DevicePeripheral.findAllByIdInList(peripheralIds)
                
                if (peripherals.size() < peripheralIds.size()) {
                    def foundIds = peripherals*.id
                    def notFoundIds = peripheralIds - foundIds
                    log.warn("DevicePeripheral(s) not found for id(s): ${notFoundIds}")
                }
                
                // Extract connected ports from peripherals
                peripherals.each { peripheral ->
                    if (peripheral.connectedTo) {
                        log.debug("Peripheral '${peripheral.name}' (id: ${peripheral.id}) connected to port: ${peripheral.connectedTo.id}")
                        ports << peripheral.connectedTo
                    } else {
                        log.warn("Peripheral '${peripheral.name}' (id: ${peripheral.id}) has no connectedTo port")
                    }
                }
                
                log.debug("Found ${peripherals.size()} peripheral(s), extracted ports from connectedTo")
            }
            
            // Remove duplicates (in case same port is referenced via both portIds and peripheralIds)
            ports = ports.unique { it.id }
            
            if (ports.isEmpty()) {
                log.warn("No valid ports found to execute action: ${params.action}")
                return
            }
            
            // Execute action on all collected ports
            log.info("Executing ${params.action} on ${ports.size()} unique port(s): ${ports*.id}")
            
            ports.each { port ->
                try {
                    log.debug("Power switch: ${port.name} (id: ${port.id}, device: ${port.device?.code}) - action: ${params.action}")
                    mqttTopicService.publish(port, [params.action])
                } catch (Exception e) {
                    log.error("Failed to publish MQTT command for port ${port.id} (${port.name}): ${e.message}", e)
                }
            }
            
            log.debug("PowerService.execute completed successfully for action: ${params.action}")
            
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in params: ${params} - ${e.message}", e)
        } catch (Exception e) {
            log.error("Unexpected error in PowerService.execute: ${e.message}", e)
        }
    }
}
