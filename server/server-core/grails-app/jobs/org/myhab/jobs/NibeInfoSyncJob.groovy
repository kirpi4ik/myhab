package org.myhab.jobs

import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortType
import org.myhab.telegram.TelegramBotHandler
import org.myhab.utils.HttpErrorUtil
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.PersistJobDataAfterExecution

import java.util.concurrent.TimeUnit

/**
 * Nibe Heat Pump myUplink API Sync Job
 * 
 * Fetches data from myUplink API v2 and publishes to MQTT for:
 * - System information (connection state, firmware, alarms)
 * - Device details (model, serial number, features)
 * - Temperature sensors (outdoor, supply, return, hot water, brine, room)
 * - Compressor parameters (frequency, state, operating times)
 * - Energy consumption and production metrics
 * - Heat pump status and operational parameters
 * 
 * Features:
 * - Uses OAuth2 tokens refreshed by NibeTokenRefreshJob (separation of concerns)
 * - Multi-step API data collection (systems, devices, firmware, points)
 * - Flexible parameter selection via database configuration
 * - Auto-creates ports only for configured parameters
 * - Comprehensive error handling per API endpoint
 * 
 * API Documentation: https://dev.myuplink.com/
 */
@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Transactional
class NibeInfoSyncJob implements Job {
    
    // myUplink API v2 Endpoints
    public static final String API_BASE_URL = "https://api.myuplink.com/v2"
    public static final String API_OAUTH_TOKEN_URL = "https://api.myuplink.com/oauth/token"
    public static final String API_SYSTEMS_ME_URL = "${API_BASE_URL}/systems/me"
    
    MqttTopicService mqttTopicService
    TelegramBotHandler telegramBotHandler
    
    String accessToken
    String deviceId
    String systemId

    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeInfoSync.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.nibeInfoSync.interval', Integer) ?: 120
        
        if (enabled == null) {
            log.debug "NibeInfoSyncJob: Configuration not found, defaulting to ENABLED"
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "NibeInfoSyncJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "NibeInfoSyncJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.nibeInfoSync.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        log.info("NibeInfoSyncJob execute() called - enabled: ${enabled}")
        
        if (!enabled) {
            log.info("NibeInfoSyncJob is DISABLED via configuration, skipping execution")
            return
        }
        
        log.info("NibeInfoSyncJob is ENABLED, proceeding with execution")
        
        def device = Device.findByModel(DeviceModel.NIBE_F1145_8_EM)
        if (!device) {
            log.error("Nibe device not found in database (model: NIBE_F1145_8_EM)")
            return
        }
        
        try {
            // Step 1: Load OAuth2 access token (refreshed by NibeTokenRefreshJob)
            if (!loadAccessToken(device)) {
                log.error("Failed to load access token, skipping sync")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            // Step 2: Fetch system information
            def systemInfo = fetchSystemInfo(device)
            if (!systemInfo) {
                log.error("Failed to fetch system info, skipping sync")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            // Step 3: Fetch and publish device firmware info
            fetchDeviceFirmware(device)
            
            // Step 4: Fetch and publish device details
            fetchDeviceDetails(device)
            
            // Step 5: Fetch and publish all sensor/parameter data points
            fetchDevicePoints(device)
            
            // Mark device as online if everything succeeded
            if (device.status == DeviceStatus.OFFLINE) {
                mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            }
            
            log.info("NibeInfoSyncJob completed successfully")
            
        } catch (Exception e) {
            log.error("NibeInfoSyncJob failed with exception: ${e.message}", e)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    /**
     * Load OAuth2 access token from database
     * Token is refreshed by NibeTokenRefreshJob every 5 minutes
     * This method simply reads the current token without calling the API
     * 
     * @param device Nibe device
     * @return true if token loaded successfully, false otherwise
     */
    boolean loadAccessToken(Device device) {
        try {
            log.debug("Loading access token from database for device ${device.id}")
            
            def accessTokenConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key()
            )
            
            if (!accessTokenConfig?.value) {
                log.error("Access token not found in database for device ${device.id}")
                log.error("Ensure NibeTokenRefreshJob is enabled and running")
                return false
            }
            
            this.accessToken = accessTokenConfig.value
            log.debug("Access token loaded successfully: ${accessToken?.take(20)}...")
            
            return true
            
        } catch (Exception e) {
            log.error("Exception loading access token: ${e.message}", e)
            return false
        }
    }

    /**
     * Fetch system information from /v2/systems/me
     * Retrieves system ID, device ID, and basic system info
     * 
     * @param device Nibe device
     * @return Map with system data or null on failure
     */
    Map fetchSystemInfo(Device device) {
        try {
            log.debug("Fetching system info from ${API_SYSTEMS_ME_URL}")
            
            HttpResponse<String> response = Unirest.get(API_SYSTEMS_ME_URL)
                .header("Authorization", "Bearer ${accessToken}")
                .asString()
            
            if (response.status != 200) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body)
                log.error("Failed to fetch system info: ${errorMsg}")
                return null
            }
            
            def data = new JsonSlurper().parseText(response.body)
            
            if (!data.systems || data.systems.isEmpty()) {
                log.error("No systems found in API response")
                return null
            }
            
            def system = data.systems[0]
            this.systemId = system.systemId
            
            if (!system.devices || system.devices.isEmpty()) {
                log.error("No devices found in system ${systemId}")
                return null
            }
            
            def deviceInfo = system.devices[0]
            this.deviceId = deviceInfo.id
            
            log.info("System found: ${system.name} (ID: ${systemId}), Device: ${deviceId}")
            
            // Publish system-level status
            publishToPort(device, 'system.connection_state', deviceInfo.connectionState)
            publishToPort(device, 'system.has_alarm', system.hasAlarm ? '1' : '0')
            
            return [
                systemId: systemId,
                systemName: system.name,
                deviceId: deviceId,
                connectionState: deviceInfo.connectionState,
                hasAlarm: system.hasAlarm
            ]
            
        } catch (Exception e) {
            log.error("Exception fetching system info: ${e.message}", e)
            return null
        }
    }

    /**
     * Fetch device firmware information from /v2/devices/{deviceId}/firmware-info
     * 
     * @param device Nibe device
     */
    void fetchDeviceFirmware(Device device) {
        try {
            def url = "${API_BASE_URL}/devices/${deviceId}/firmware-info"
            log.debug("Fetching firmware info from ${url}")
            
            HttpResponse<String> response = Unirest.get(url)
                .header("Authorization", "Bearer ${accessToken}")
                .asString()
            
            if (response.status != 200) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body)
                log.error("Failed to fetch firmware info: ${errorMsg}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            def data = new JsonSlurper().parseText(response.body)
            
            publishToPort(device, 'system.firmware_version', data.currentFwVersion)
            publishToPort(device, 'system.desired_firmware', data.desiredFwVersion ?: data.currentFwVersion)
            
            log.debug("Firmware: current=${data.currentFwVersion}, desired=${data.desiredFwVersion}")
            
        } catch (Exception e) {
            log.error("Exception fetching firmware info: ${e.message}", e)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    /**
     * Fetch device details from /v2/devices/{deviceId}
     * 
     * @param device Nibe device
     */
    void fetchDeviceDetails(Device device) {
        try {
            def url = "${API_BASE_URL}/devices/${deviceId}"
            log.debug("Fetching device details from ${url}")
            
            HttpResponse<String> response = Unirest.get(url)
                .header("Authorization", "Bearer ${accessToken}")
                .asString()
            
            if (response.status != 200) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body)
                log.error("Failed to fetch device details: ${errorMsg}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            def data = new JsonSlurper().parseText(response.body)
            
            publishToPort(device, 'system.connection_state', data.connectionState)
            
            if (data.firmware) {
                publishToPort(device, 'system.firmware_version', data.firmware.currentFwVersion)
                publishToPort(device, 'system.desired_firmware', data.firmware.desiredFwVersion)
            }
            
            log.debug("Device details: ${data.product?.name}, connection=${data.connectionState}")
            
        } catch (Exception e) {
            log.error("Exception fetching device details: ${e.message}", e)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    /**
     * Fetch all device data points from /v2/devices/{deviceId}/points
     * This is the main data collection method that retrieves all sensor values
     * 
     * @param device Nibe device
     */
    void fetchDevicePoints(Device device) {
        try {
            def url = "${API_BASE_URL}/devices/${deviceId}/points"
            log.debug("Fetching device points from ${url}")
            
            HttpResponse<String> response = Unirest.get(url)
                .header("Authorization", "Bearer ${accessToken}")
                .asString()
            
            if (response.status != 200) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body)
                log.error("Failed to fetch device points: ${errorMsg}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            def points = new JsonSlurper().parseText(response.body)
            
            if (!points || !points instanceof List) {
                log.error("Invalid points data received")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return
            }
            
            log.info("Processing ${points.size()} data points")
            
            int publishedCount = 0
            int skippedCount = 0
            
            points.each { point ->
                if (point.parameterId) {
                    def port = findOrCreatePort(device, point)
                    
                    if (port) {
                        // Use the raw value for publishing
                        def value = point.value
                        if (value != null) {
                            try {
                                mqttTopicService.publish(port, value.toString())
                                publishedCount++
                                
                                // Add small delay to avoid overwhelming MQTT broker
                                // Publishing 24 parameters every 30s needs throttling
                                Thread.sleep(50) // 50ms delay between publishes
                            } catch (Exception ex) {
                                log.error("Failed to publish port ${port.internalRef}: ${ex.message}")
                            }
                        }
                    } else {
                        skippedCount++
                    }
                }
            }
            
            log.info("Published ${publishedCount} points, skipped ${skippedCount} unconfigured points")
            
        } catch (Exception e) {
            log.error("Exception fetching device points: ${e.message}", e)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }
    }

    /**
     * Find existing port or create new port for parameter
     * Only creates ports for parameters configured in database
     * 
     * @param device Nibe device
     * @param point Point data from API
     * @return DevicePort or null if parameter not configured
     */
    DevicePort findOrCreatePort(Device device, Map point) {
        def parameterId = point.parameterId
        
        // Find existing port by internal reference (parameterId)
        def port = device.ports?.find { it.internalRef == parameterId }
        
        if (port) {
            return port
        }
        
        // Check if this parameter is configured for monitoring
        def configuredParams = getConfiguredParameters(device)
        
        if (!configuredParams.contains(parameterId)) {
            // Parameter not configured, skip port creation
            return null
        }
        
        // Auto-create port for configured parameter
        try {
            log.info("Auto-creating port for parameter ${parameterId}: ${point.parameterName}")
            
            // Build description with unit if available
            def description = point.category ?: "Nibe parameter"
            if (point.parameterUnit) {
                description += " (${point.parameterUnit})"
            }
            
            port = new DevicePort(
                device: device,
                internalRef: parameterId,
                name: point.parameterName ?: "Parameter ${parameterId}",
                description: description,
                type: determinePortType(point)
            )
            
            port.save(flush: true, failOnError: true)
            
            // Refresh device ports collection
            device.refresh()
            
            log.debug("Created port ${port.id} for parameter ${parameterId}")
            return port
            
        } catch (Exception e) {
            log.error("Failed to create port for parameter ${parameterId}: ${e.message}")
            return null
        }
    }

    /**
     * Get list of all configured parameters for monitoring
     * Reads from NIBE_*_PARAMS configurations
     * 
     * @param device Nibe device
     * @return Set of parameter IDs
     */
    Set<String> getConfiguredParameters(Device device) {
        def params = new HashSet<String>()
        
        // Get all parameter configuration keys
        def paramConfigs = [
            'NIBE_TEMP_PARAMS',
            'NIBE_COMPRESSOR_PARAMS',
            'NIBE_ENERGY_PARAMS',
            'NIBE_STATUS_PARAMS'
        ]
        
        paramConfigs.each { configKey ->
            def config = Configuration.findByEntityIdAndEntityTypeAndKey(
                device.id,
                EntityType.DEVICE,
                configKey
            )
            if (config?.value) {
                // Split comma-separated parameter IDs
                config.value.split(',').each { paramId ->
                    params.add(paramId.trim())
                }
            }
        }
        
        log.debug("Configured parameters (${params.size()}): ${params}")
        return params
    }

    /**
     * Determine port type based on parameter metadata
     * 
     * @param point Point data from API
     * @return PortType enum value
     */
    PortType determinePortType(Map point) {
        // All Nibe parameters are SENSOR type
        // The actual data type (temperature, energy, etc.) is handled by the unit field
        return PortType.SENSOR
    }

    /**
     * Publish value to device port (finds port by internal reference)
     * 
     * @param device Nibe device
     * @param internalRef Port internal reference
     * @param value Value to publish
     */
    void publishToPort(Device device, String internalRef, String value) {
        if (value == null) return
        
        def port = device.ports?.find { it.internalRef == internalRef }
        
        if (port) {
            mqttTopicService.publish(port, value)
        } else {
            log.trace("Port not found for internal ref: ${internalRef}")
        }
    }
}
