package org.myhab.jobs

import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
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
import org.myhab.domain.device.port.PortState
import org.myhab.utils.HttpErrorUtil
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

/**
 * Huawei FusionSolar API Sync Job
 * 
 * Fetches data from Huawei FusionSolar API and publishes to MQTT for:
 * - Station-level aggregated data (daily/monthly/total yields)
 * - Inverter real-time data (solar production, efficiency)
 * - Power meter data (grid import/export, per-phase consumption)
 * 
 * Features:
 * - Single login per run, reuses token for all API calls
 * - Flexible parameter selection via database configuration
 * - Auto-creates ports only for configured parameters
 * - Comprehensive error handling per API endpoint
 */
@Slf4j
@DisallowConcurrentExecution
class HuaweiInfoSyncJob implements Job {
    
    // API Endpoints (see https://support.huawei.com/enterprise/en/doc/EDOC1100316813)
    public static final String API_DEVICE_LIST_URL = "https://eu5.fusionsolar.huawei.com/thirdData/getDevList"
    public static final String API_STATION_REAL_KPI_URL = "https://eu5.fusionsolar.huawei.com/thirdData/getStationRealKpi"
    public static final String API_DEV_REAL_KPI_URL = "https://eu5.fusionsolar.huawei.com/thirdData/getDevRealKpi"
    
    // Device Type IDs
    public static final int DEV_TYPE_INVERTER = 1
    public static final int DEV_TYPE_METER = 47
    public static final int DEV_TYPE_DONGLE = 62

    MqttTopicService mqttTopicService
    String token
    String stationCode

    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.huaweiInfoSync.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.huaweiInfoSync.interval', Integer) ?: 120
        
        if (enabled == null) {
            log.debug "HuaweiInfoSyncJob: Configuration not found, defaulting to DISABLED"
            enabled = false
        }
        
        if (enabled) {
            log.debug "HuaweiInfoSyncJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "HuaweiInfoSyncJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.huaweiInfoSync.enabled', Boolean)
        
        if (enabled == null) {
            enabled = false
        }
        
        log.trace("HuaweiInfoSyncJob execute() called - enabled: ${enabled}")
        
        if (!enabled) {
            log.trace("HuaweiInfoSyncJob is DISABLED via configuration, skipping execution")
            return
        }
        
        log.info("HuaweiInfoSyncJob is ENABLED, proceeding with execution")
        
        try {
            // Step 1: Login once and get token
            if (!login()) {
                log.error("Login failed, skipping sync")
                return
            }
            
            // Step 2: Fetch station-level data (aggregated totals)
            def inverterDevice = Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2)
            if (inverterDevice) {
                fetchStationData(inverterDevice)
                sleep(5000) // Small delay between API calls
                
                // Step 3: Fetch inverter real-time data
                fetchDeviceRealKpi(inverterDevice, DEV_TYPE_INVERTER, 'DEV_IDS', 'INVERTER_API_PARAMS', 'inverter')
                sleep(5000)
                
                // Step 4: Fetch power meter data
                def meterDevice = Device.findByModel(DeviceModel.ELECTRIC_METER_DTS)
                if (meterDevice) {
                    // Try meter device's DEV_IDS first, fallback to inverter's METER_DEV_IDS
                    def meterDevIds = getConfigValue(meterDevice, 'DEV_IDS') ?: getConfigValue(inverterDevice, 'METER_DEV_IDS')
                    if (meterDevIds) {
                        fetchDeviceRealKpi(meterDevice, DEV_TYPE_METER, meterDevIds, 'METER_API_PARAMS', 'meter')
                    } else {
                        log.error("Missing DEV_IDS or METER_DEV_IDS configuration for meter")
                    }
                } else {
                    log.warn("Electric meter device not found")
                }
            } else {
                log.error("Huawei inverter device not found (model: ${DeviceModel.HUAWEI_SUN2000_12KTL_M2})")
            }
            
            log.info("HuaweiInfoSyncJob completed successfully")
        } catch (Exception ex) {
            log.error("HuaweiInfoSyncJob failed", ex)
            throw new JobExecutionException(ex)
        }
    }

    /**
     * Login to Huawei FusionSolar API
     * @return true if login successful, false otherwise
     */
    boolean login() {
        def device = Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2)
        if (!device) {
            log.error("Inverter device not found for login")
            return false
        }
        
        try {
            // Use the direct configuration lookup method
            def user = getConfigValue(device, CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_USER.key())
            def passwd = getConfigValue(device, CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_PASSWD.key())
            def apiLoginUrl = getConfigValue(device, 'API_LOGIN_URL')
            def tokenHeader = getConfigValue(device, 'TOKEN_HEADER')
            stationCode = getConfigValue(device, 'STATION_CODE')
            
            if (!apiLoginUrl || !tokenHeader || !stationCode) {
                log.error("Missing configuration: API_LOGIN_URL, TOKEN_HEADER, or STATION_CODE")
                return false
            }
            
            if (!user || !passwd) {
                log.error("Missing OAuth credentials")
                return false
            }
            
            HttpResponse<JsonNode> response = Unirest.post(apiLoginUrl)
                    .header("Content-Type", "application/json")
                    .body("{\"userName\": \"${user}\",\"systemCode\": \"${passwd}\"}")
                    .asJson()
                    
            if (response.success) {
                def tokens = response.getHeaders().get(tokenHeader)
                if (tokens && !tokens.isEmpty()) {
                    token = tokens.first()
                    log.info("Login successful, token obtained")
                    return true
                } else {
                    log.error("Login response missing token header: ${tokenHeader}")
                    return false
                }
            } else {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body?.toString() ?: "")
                log.error("Login failed: ${errorMsg}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
                return false
            }
        } catch (Exception ex) {
            log.error("Login exception: ${ex.message}", ex)
            if (device) {
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
            }
            return false
        }
    }

    /**
     * Fetch station-level aggregated data (totals)
     * API: getStationRealKpi
     */
    void fetchStationData(Device device) {
        if (!token) {
            log.warn("No token available for fetchStationData")
            return
        }
        
        try {
            HttpResponse<String> response = Unirest.post(API_STATION_REAL_KPI_URL)
                    .header("Content-Type", "application/json")
                    .header("XSRF-TOKEN", token)
                    .body("{\"stationCodes\":\"${stationCode}\"}")
                    .asString()
                    
            if (!response.success) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body ?: "")
                log.error("Station API failed: ${errorMsg}")
                return
            }
            
            def jsonResponse = new JsonSlurper().parseText(response.body)
            if (jsonResponse["failCode"] == 305) {
                log.error("Station API token expired (failCode 305)")
                return
            }
            
            if (!jsonResponse["success"]) {
                log.error("Station API returned success=false: ${jsonResponse}")
                return
            }
            
            // Get configured parameters to track
            def paramsToTrack = getConfiguredParameters(device, 'STATION_API_PARAMS')
            if (!paramsToTrack) {
                log.warn("No STATION_API_PARAMS configured, skipping station data")
                return
            }
            
            def dataItemMap = jsonResponse["data"]?.first()?["dataItemMap"]
            if (!dataItemMap) {
                log.warn("Station API returned no data")
                return
            }
            
            publishParameters(device, 'station', dataItemMap, paramsToTrack)
            mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            
            log.info("Station data synced: ${paramsToTrack.size()} parameters")
        } catch (Exception ex) {
            log.error("Failed to fetch station data", ex)
        }
    }

    /**
     * Fetch device real-time data from getDevRealKpi API
     * Consolidated method for inverter and meter data
     * 
     * @param device Device to fetch data for
     * @param devTypeId Device type ID (1=inverter, 47=meter, 62=dongle)
     * @param devIdsConfigKey Configuration key for device IDs, or direct value
     * @param paramsConfigKey Configuration key for parameters list
     * @param prefix Prefix for port naming (e.g., 'inverter', 'meter')
     */
    void fetchDeviceRealKpi(Device device, int devTypeId, String devIdsConfigKey, String paramsConfigKey, String prefix) {
        if (!token) {
            log.warn("No token available for ${prefix} data fetch")
            return
        }
        
        try {
            // devIdsConfigKey can be either a config key or direct value
            def devIds = devIdsConfigKey.startsWith('DEV_') ? getConfigValue(device, devIdsConfigKey) : devIdsConfigKey
            if (!devIds) {
                log.error("Missing device IDs configuration for ${prefix}")
                return
            }
            
            HttpResponse<String> response = Unirest.post(API_DEV_REAL_KPI_URL)
                    .header("Content-Type", "application/json")
                    .header("XSRF-TOKEN", token)
                    .body("{\"devTypeId\":${devTypeId},\"devIds\":\"${devIds}\"}")
                    .asString()
                    
            if (!response.success) {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body ?: "")
                log.error("${prefix.capitalize()} API failed: ${errorMsg}")
                return
            }
            
            def jsonResponse = new JsonSlurper().parseText(response.body)
            if (jsonResponse["failCode"] == 305) {
                log.error("${prefix.capitalize()} API token expired (failCode 305)")
                return
            }
            
            if (!jsonResponse["success"]) {
                log.error("${prefix.capitalize()} API returned success=false: ${jsonResponse}")
                return
            }
            
            def paramsToTrack = getConfiguredParameters(device, paramsConfigKey)
            if (!paramsToTrack) {
                log.warn("No ${paramsConfigKey} configured, skipping ${prefix} data")
                return
            }
            
            def dataItemMap = jsonResponse["data"]?.first()?["dataItemMap"]
            if (!dataItemMap) {
                log.warn("${prefix.capitalize()} API returned no data")
                return
            }
            
            publishParameters(device, prefix, dataItemMap, paramsToTrack)
            mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
            
            log.info("${prefix.capitalize()} data synced: ${paramsToTrack.size()} parameters")
        } catch (Exception ex) {
            log.error("Failed to fetch ${prefix} data", ex)
        }
    }

    /**
     * Publish parameters to MQTT
     * Only publishes parameters that are in the configured list
     * 
     * @param device Device to publish for
     * @param prefix Prefix for port naming (e.g., 'station', 'inverter', 'meter')
     * @param dataMap API response data map
     * @param paramsToTrack List of parameter names to publish
     */
    void publishParameters(Device device, String prefix, Map dataMap, List<String> paramsToTrack) {
        int publishedCount = 0
        int skippedCount = 0
        boolean portsCreated = false
        
        paramsToTrack.each { paramName ->
            def value = dataMap[paramName]
            
            if (value != null) {
                def internalRef = "${prefix}.${paramName}"
                
                try {
                    DevicePort port = device.ports.find { it.internalRef == internalRef } as DevicePort
                    
                    if (port == null) {
                        // Auto-create port for configured parameter
                        try {
                            port = DevicePort.withNewTransaction { status ->
                                def newPort = new DevicePort(
                                    device: device,
                                    type: PortType.SENSOR,
                                    state: PortState.ACTIVE,
                                    internalRef: internalRef,
                                    name: formatPortName(paramName),
                                    description: "Auto-created ${prefix} parameter: ${paramName}"
                                )
                                newPort.save(flush: true, failOnError: true)
                                return newPort
                            }
                            log.info("Created new port: ${internalRef}")
                            portsCreated = true
                        } catch (Exception ex) {
                            log.error("Failed to create port: ${internalRef}", ex)
                            skippedCount++
                            return
                        }
                    }
                    
                    // Publish to MQTT
                    mqttTopicService.publishPortValue(device, port, value as String)
                    publishedCount++
                    
                    // Small delay to avoid overwhelming MQTT broker
                    if (publishedCount % 10 == 0) {
                        Thread.sleep(10)
                    }
                } catch (Exception ex) {
                    log.error("Failed to publish parameter: ${internalRef}", ex)
                    skippedCount++
                }
            } else {
                log.trace("Parameter ${paramName} is null, skipping")
                skippedCount++
            }
        }
        
        // Refresh device once at the end if any ports were created
        if (portsCreated) {
            device.refresh()
        }
        
        log.debug("Published ${publishedCount} parameters, skipped ${skippedCount}")
    }

    /**
     * Get list of configured parameters to track for an API
     * Parameters are stored as comma-separated values in configuration
     * 
     * Example: "day_power,month_power,total_power"
     * 
     * @param device Device to get config from
     * @param configKey Configuration key (e.g., 'STATION_API_PARAMS')
     * @return List of parameter names, or empty list if not configured
     */
    List<String> getConfiguredParameters(Device device, String configKey) {
        def paramsString = getConfigValue(device, configKey)
        
        if (!paramsString) {
            return []
        }
        
        return paramsString.split(',')
                .collect { it.trim() }
                .findAll { it }  // Remove empty strings
    }

    /**
     * Format parameter name for display
     * Converts snake_case to Title Case
     * 
     * Example: "day_power" -> "Day Power"
     */
    String formatPortName(String paramName) {
        return paramName.split('_')
                .collect { it.capitalize() }
                .join(' ')
    }

    /**
     * Get configuration value by key from device
     */
    String getConfigValue(Device device, String key) {
        if (!device) {
            return null
        }
        
        def config = Configuration.findByEntityIdAndEntityTypeAndKey(
            device.id,
            EntityType.DEVICE,
            key
        )
        
        return config?.value
    }
}
