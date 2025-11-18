package org.myhab.jobs

import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceCategory
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortState
import org.myhab.domain.device.port.PortType
import org.myhab.utils.HttpErrorUtil
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Job to sync weather data from Open-Meteo API
 * Runs hourly to fetch weather forecast data and publish to MQTT
 */
@Slf4j
@DisallowConcurrentExecution
class MeteoStationSyncJob implements Job {
    
    MqttTopicService mqttTopicService

    static triggers = {
        // Read configuration for job enablement and interval
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.meteoStationSync.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.meteoStationSync.interval', Integer) ?: 3600 // Default 1 hour
        
        // If enabled is null, configuration not found - default to false for safety
        if (enabled == null) {
            log.debug "MeteoStationSyncJob: Configuration not found, defaulting to DISABLED"
            enabled = false
        }
        
        if (enabled) {
            log.debug "MeteoStationSyncJob: ENABLED - Registering trigger with interval ${interval}s"
            simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "MeteoStationSyncJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        // Runtime check: respect configuration even if trigger was already registered
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.meteoStationSync.enabled', Boolean)
        
        // If enabled is null, configuration not found - default to false for safety
        if (enabled == null) {
            enabled = false
        }
        
        log.info("MeteoStationSyncJob execute() called - enabled: ${enabled}")
        
        if (!enabled) {
            log.info("MeteoStationSyncJob is DISABLED via configuration, skipping execution")
            return
        }
        
        log.info("MeteoStationSyncJob is ENABLED, proceeding with execution")
        
        // Find meteo station device
        def meteoCategory = DeviceCategory.findByName('METEO_STATION')
        if (!meteoCategory) {
            log.error("METEO_STATION category not found. Please run the SQL setup script.")
            return
        }
        
        def meteoDevice = Device.findByType(meteoCategory)
        if (!meteoDevice) {
            log.error("Meteo station device not found. Please run the SQL setup script.")
            return
        }
        
        try {
            syncWeatherData(meteoDevice)
        } catch (Exception ex) {
            log.error("Failed to sync weather data", ex)
            mqttTopicService.publishStatus(meteoDevice, DeviceStatus.OFFLINE)
        }
    }

    /**
     * Sync weather data from Open-Meteo API
     */
    void syncWeatherData(Device device) {
        log.info("Syncing weather data for device: ${device.name}")
        
        // Get configuration values
        def apiUrl = getConfigValue(device, 'API_URL')
        def latitude = getConfigValue(device, 'latitude')
        def longitude = getConfigValue(device, 'longitude')
        def daily = getConfigValue(device, 'daily')
        def hourly = getConfigValue(device, 'hourly')
        def current = getConfigValue(device, 'current')
        def timezone = getConfigValue(device, 'timezone')
        def forecastDays = getConfigValue(device, 'forecast_days')
        
        if (!apiUrl || !latitude || !longitude) {
            log.error("Missing required configuration: API_URL, latitude, or longitude")
            return
        }
        
        // Build API URL with parameters (URL-encoded)
        def requestUrl = "${apiUrl}?" +
            "latitude=${urlEncode(latitude)}&" +
            "longitude=${urlEncode(longitude)}&" +
            "timezone=${urlEncode(timezone ?: 'GMT')}&" +
            "forecast_days=${urlEncode(forecastDays ?: '3')}"
        
        if (daily) {
            requestUrl += "&daily=${urlEncode(daily)}"
        }
        
        if (hourly) {
            requestUrl += "&hourly=${urlEncode(hourly)}"
        }
        
        if (current) {
            requestUrl += "&current=${urlEncode(current)}"
        }
        
        log.info("Fetching weather data from: ${requestUrl}")
        
        try {
            HttpResponse<String> response = Unirest.get(requestUrl)
                .header("Accept", "application/json")
                .asString()
            
            if (response.success) {
                parseAndPublishWeatherData(device, response.body)
                mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
                log.info("Weather data synced successfully for device: ${device.name}")
            } else {
                String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body ?: "")
                log.error("Failed to fetch weather data: ${errorMsg}")
                mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
            }
        } catch (Exception ex) {
            log.error("Error fetching weather data from API", ex)
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
            throw ex
        }
    }

    /**
     * Parse API response and publish to MQTT
     */
    void parseAndPublishWeatherData(Device device, String responseBody) {
        def data = new JsonSlurper().parseText(responseBody)
        
        if (!data) {
            log.warn("Empty response from weather API")
            return
        }
        
        // Publish root-level parameters
        publishParameter(device, 'latitude', data.latitude)
        publishParameter(device, 'longitude', data.longitude)
        publishParameter(device, 'elevation', data.elevation)
        publishParameter(device, 'timezone', data.timezone)
        publishParameter(device, 'timezone_abbreviation', data.timezone_abbreviation)
        
        // Publish current weather parameters (single values, not arrays)
        // Add small delay between publishes to avoid overwhelming MQTT broker
        if (data.current) {
            data.current.eachWithIndex { key, value, index ->
                publishParameter(device, "current.${key}", value)
                if (index < data.current.size() - 1) {
                    Thread.sleep(10) // 10ms delay between publishes
                }
            }
        }
        
        // Publish daily parameters (arrays for forecast)
        if (data.daily) {
            data.daily.eachWithIndex { key, value, index ->
                publishParameter(device, "daily.${key}", value)
                if (index < data.daily.size() - 1) {
                    Thread.sleep(10) // 10ms delay between publishes
                }
            }
        }
        
        // Publish hourly parameters (arrays for forecast)
        if (data.hourly) {
            data.hourly.eachWithIndex { key, value, index ->
                publishParameter(device, "hourly.${key}", value)
                if (index < data.hourly.size() - 1) {
                    Thread.sleep(10) // 10ms delay between publishes
                }
            }
        }
        
        def portCount = countPublishedPorts(data)
        log.info("Saved and published ${portCount} weather parameters")
    }

    /**
     * Publish a single parameter to MQTT
     * Creates port if it doesn't exist (in its own transaction)
     * Value will be persisted to database by PortValueService via MQTT event flow
     */
    private void publishParameter(Device device, String internalRef, Object value) {
        if (value == null) {
            return
        }
        
        DevicePort port = device.ports.find { it.internalRef == internalRef } as DevicePort
        
        if (port == null) {
            log.debug("Creating new port for parameter: ${internalRef}")
            
            // Create port in its own transaction to prevent cascading failures
            try {
                port = DevicePort.withNewTransaction { status ->
                    def newPort = new DevicePort(
                        device: device,
                        type: PortType.SENSOR,
                        state: PortState.ACTIVE,
                        internalRef: internalRef,
                        name: formatPortName(internalRef),
                        description: "Auto-created weather parameter",
                        uid: null  // Set to null instead of empty string to avoid unique constraint violations
                    )
                    newPort.save(flush: true, failOnError: true)
                    return newPort
                }
                log.info("Created new port: ${internalRef}")
                // Refresh device ports collection
                device.refresh()
            } catch (Exception ex) {
                log.error("Failed to create port: ${internalRef}", ex)
                return
            }
        }
        
        // Convert value to string for MQTT publishing
        def valueStr = convertValueToString(value)
        
        try {
            // Publish to MQTT - will be received by MQTTMessageHandler
            // which triggers evt_mqtt_port_value_changed event
            // which is handled by PortValueService to persist value
            mqttTopicService.publishPortValue(device, port, valueStr)
            log.trace("Published ${internalRef}")
        } catch (Exception ex) {
            log.error("Failed to publish value for port: ${internalRef}", ex)
        }
    }

    /**
     * Get configuration value by key
     */
    private String getConfigValue(Device device, String configKey) {
        def config = Configuration.where {
            entityId == device.id && 
            entityType == EntityType.DEVICE && 
            key == configKey
        }.find()
        
        return config?.value
    }
    
    /**
     * URL-encode a parameter value
     */
    private String urlEncode(String value) {
        if (!value) {
            return ''
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    /**
     * Format internal ref to human-readable name
     */
    private String formatPortName(String internalRef) {
        return internalRef
            .replaceAll(/[._]/, ' ')
            .split(' ')
            .collect { it.capitalize() }
            .join(' ')
    }

    /**
     * Convert various value types to string for MQTT
     */
    private String convertValueToString(Object value) {
        if (value instanceof List) {
            // For arrays, convert to JSON string
            return groovy.json.JsonOutput.toJson(value)
        } else if (value instanceof Map) {
            // For objects, convert to JSON string
            return groovy.json.JsonOutput.toJson(value)
        } else {
            return value.toString()
        }
    }

    /**
     * Count total number of parameters in response
     */
    private int countPublishedPorts(Map data) {
        int count = 0
        
        // Root level
        ['latitude', 'longitude', 'elevation', 'timezone', 'timezone_abbreviation'].each {
            if (data[it]) count++
        }
        
        // Current
        if (data.current) {
            count += data.current.size()
        }
        
        // Daily
        if (data.daily) {
            count += data.daily.size()
        }
        
        // Hourly
        if (data.hourly) {
            count += data.hourly.size()
        }
        
        return count
    }
}

