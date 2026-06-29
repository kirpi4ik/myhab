package org.myhab.services.dsl.action


import org.myhab.ConfigKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.events.AuditSource
import org.myhab.init.cache.CacheMap
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import java.util.concurrent.TimeUnit

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
    def auditService
    def hazelcastInstance

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

            // One-shot timeout override for switchOn([..., timeout: <sec>]). Stored as a
            // Configuration row (key=key.on.timeout.override) on each port's first peripheral
            // — visible in the UI alongside the regular key.on.timeout, traceable, and
            // consumed (then deleted) by PortValueService.updateExpirationTime when the
            // device confirms ON. Wins over the persisted key.on.timeout for that one cycle.
            Integer customTimeoutSec = (params.timeout instanceof Number) ? (params.timeout as Integer) : null
            if (params.action == PortAction.ON && customTimeoutSec != null && customTimeoutSec > 0) {
                ports.each { port ->
                    def peripheral = port.peripherals?.find()
                    if (peripheral == null) {
                        log.debug("Skipping timeout override for port ${port.id}: no peripheral attached")
                        return
                    }
                    try {
                        upsertTimeoutOverride(peripheral, customTimeoutSec)
                        log.debug("Registered timeout override for peripheral ${peripheral.id} (port ${port.id}): ${customTimeoutSec}s")
                    } catch (Exception e) {
                        log.error("Failed to register timeout override for peripheral ${peripheral.id}: ${e.message}", e)
                    }
                }
            }

            // Synchronous override cleanup on caller-initiated OFF — covers the case where
            // the device was already OFF and won't echo the change via MQTT, so the
            // updateExpirationTime OFF cleanup never fires. Idempotent: deleting a
            // non-existent row is a no-op.
            if (params.action == PortAction.OFF) {
                ports.each { port ->
                    def peripheral = port.peripherals?.find()
                    if (peripheral == null) return
                    try {
                        clearTimeoutOverride(peripheral)
                    } catch (Exception e) {
                        log.error("Failed to clear pending timeout override for peripheral ${peripheral.id}: ${e.message}", e)
                    }
                }
            }

            // Audit context — who/what initiated this command. Defaults keep
            // legacy callers working (SYSTEM); real callers thread these in.
            AuditSource source = AuditSource.from(params.source)
            String actor = params.actor ?: 'SYSTEM'

            ports.each { port ->
                Long peripheralId = port.peripherals?.find()?.id
                // Correlation id linking this command to the device's MQTT echo.
                String actionId = UUID.randomUUID().toString()
                try {
                    log.debug("Power switch: ${port.name} (id: ${port.id}, device: ${port.device?.code}) - action: ${params.action}")
                    mqttTopicService.publish(port, [params.action])
                    // Park the actionId so PortValueService can stamp the same id on
                    // the device-confirmation row (the device can't echo it itself).
                    storePendingAction(port.id, params.action, actionId)
                    // Audit the real, executed command — one row per resolved
                    // port, reflecting what actually happened (unresolved ports
                    // were already filtered out above, so they produce no row).
                    writeAudit(port.id, params.action, source, actor, [peripheralId: peripheralId, actionId: actionId])
                } catch (Exception e) {
                    log.error("Failed to publish MQTT command for port ${port.id} (${port.name}): ${e.message}", e)
                    // No pending entry on failure — no echo is expected.
                    writeAudit(port.id, params.action, source, actor,
                            [peripheralId: peripheralId, status: 'FAILED', error: e.message, actionId: actionId])
                }
            }
            
            log.debug("PowerService.execute completed successfully for action: ${params.action}")
            
        } catch (NumberFormatException e) {
            log.error("Invalid ID format in params: ${params} - ${e.message}", e)
        } catch (Exception e) {
            log.error("Unexpected error in PowerService.execute: ${e.message}", e)
        }
    }

    /**
     * Upsert the one-shot timeout override Configuration row for a peripheral.
     * Wraps in a writable transaction since the surrounding service is readOnly.
     *
     * @param peripheral target peripheral
     * @param timeoutSec override value, in seconds (must be &gt; 0)
     */
    /**
     * Write one audit row for an executed (or failed) port command. A failure to
     * write the audit row is logged but never aborts the MQTT batch.
     */
    private void writeAudit(Long portId, PortAction action, AuditSource source, String actor, Map details) {
        try {
            auditService.logStateChange(EntityType.PORT, portId, action, source, actor, details)
        } catch (Exception e) {
            log.error("Failed to write audit row for port ${portId}: ${e.message}", e)
        }
    }

    /**
     * Park the actionId for an in-flight command so the device's MQTT state echo can
     * be correlated back to it. Keyed by "portId:action" with a short per-entry TTL
     * (the entry self-clears if the device never confirms). A Hazelcast failure must
     * not abort the command batch.
     */
    private void storePendingAction(Long portId, PortAction action, String actionId) {
        try {
            hazelcastInstance?.getMap(CacheMap.PENDING_ACTION.name)
                    ?.put("${portId}:${action.name()}".toString(), [actionId: actionId], 30, TimeUnit.SECONDS)
        } catch (Exception e) {
            log.warn("Failed to store pending action for port ${portId}: ${e.message}")
        }
    }

    private void upsertTimeoutOverride(DevicePeripheral peripheral, Integer timeoutSec) {
        Configuration.withNewTransaction {
            Configuration existing = Configuration.where {
                entityId == peripheral.id &&
                        entityType == EntityType.PERIPHERAL &&
                        key == ConfigKey.STATE_ON_TIMEOUT_OVERRIDE
            }.find()
            if (existing == null) {
                existing = new Configuration(
                        entityId: peripheral.id,
                        entityType: EntityType.PERIPHERAL,
                        key: ConfigKey.STATE_ON_TIMEOUT_OVERRIDE
                )
            }
            existing.value = String.valueOf(timeoutSec)
            existing.save(flush: true, failOnError: true)
        }
    }

    /**
     * Remove any pending one-shot timeout override Configuration row for a peripheral.
     * Idempotent — silently does nothing when no row exists. Wraps in a writable
     * transaction since the surrounding service is readOnly.
     */
    private void clearTimeoutOverride(DevicePeripheral peripheral) {
        Configuration.withNewTransaction {
            int deleted = Configuration.where {
                entityId == peripheral.id &&
                        entityType == EntityType.PERIPHERAL &&
                        key == ConfigKey.STATE_ON_TIMEOUT_OVERRIDE
            }.deleteAll()
            if (deleted > 0) {
                log.debug("Cleared pending timeout override for peripheral ${peripheral.id} on OFF")
            }
        }
    }
}
