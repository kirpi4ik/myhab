package org.myhab.jobs


import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.TopicName
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.EventData
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

import static org.myhab.ConfigKey.CONFIG_LIST_SCHEDULED_TEMP
import static org.myhab.ConfigKey.CONFIG_TEMP_ALL_DAY

/**
 *
 */
@Slf4j
@DisallowConcurrentExecution
class HeatingControlJob implements Job, EventPublisher {
    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.heatingControl.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.heatingControl.interval', Integer) ?: 120
        
        if (enabled == null) {
            enabled = true  // Default to enabled for backward compatibility
        }
        
        if (enabled) {
            log.debug "HeatingControlJob: ENABLED - Registering trigger with interval ${interval}s"
            simple name: 'heatControlJob', repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            log.debug "HeatingControlJob: DISABLED - Not registering trigger"
        }
    }
    public static final String PERIPHERAL_HEAT_CTRL_CATEGORY = "HEAT"
    public static final String PERIPHERAL_TEMPERATURE_SENSOR_CATEGORY = 'TEMP'
    def configProvider

    static group = "Internal"
    static description = "Heat control"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.heatingControl.enabled', Boolean)
        
        if (enabled == null) {
            enabled = true
        }
        
        if (!enabled) {
            log.info("HeatingControlJob is DISABLED via configuration, skipping execution")
            return
        }
        if (configProvider.get(Boolean.class, "heat.thermostat.enabled")) {
            def actions = [:]
            Set<Zone> allzones = Zone.findAll()
            def zonesWithScheduledTemp = allzones.findAll { zone -> zone.getConfigurations().find { configuration -> configuration.key == CONFIG_LIST_SCHEDULED_TEMP } != null }.sort { zone -> zone.parent }.reverse()
            zonesWithScheduledTemp.each { zone ->
                def desiredTempForActuator = getDesiredTemperature(zone)
                def currentTemperatures = getCurrentTempSInZone(zone)
                def currentTemp
                if (desiredTempForActuator != null) {
                    if (currentTemperatures.size() > 0) {
                        //there is at least one thermo sensor
                        currentTemp = currentTemperatures[0]
                    } else {
                        currentTemp = 0
                    }
                    Set<DevicePeripheral> actuatorHeatCtrlSet = DevicePeripheral.findAll("select dp from  DevicePeripheral dp where ?0 in elements(zones) and dp.category.name = ?1", [zone], PERIPHERAL_HEAT_CTRL_CATEGORY)
                    actuatorHeatCtrlSet.each { actuator ->
                        if (currentTemp <= desiredTempForActuator) {
                            actions << ["${actuator.id}": PortAction.ON]
                        } else {
                            actions << ["${actuator.id}": PortAction.OFF]

                        }
                    }

                }
            }
            actions.each { actuatorId, action ->
                publish(TopicName.EVT_HEAT.id(), new EventData().with {
                    p0 = TopicName.EVT_HEAT.id()
                    p1 = EntityType.PERIPHERAL.name()
                    p2 = actuatorId
                    p3 = "thermostat"
                    p4 = action
                    p6 = "system"
                    it
                })
            }
        }
    }

    def getCurrentTempSInZone(Zone zone) {
        def currentTemp = []
        DevicePeripheral.createCriteria().list {
            category {
                eq('name', PERIPHERAL_TEMPERATURE_SENSOR_CATEGORY)
            }
            zones {
                idEq(zone.id)
            }
        }.each { deviceThermoPeripheral ->
            deviceThermoPeripheral.connectedTo.findAll { termostatPort -> termostatPort.value?.isNumber() }.each { termostatPort -> currentTemp << Double.valueOf(termostatPort.value) }
        }
        return currentTemp
    }

    def allChildZones(Zone zone) {
        def zones = []
        for (Zone zn : zone.zones) {
            if (zn.zones.isEmpty()) {
                zones << zn
            } else {
                zones << allChildZones(zn)
            }
        }
        return zones
    }

    def getDesiredTemperature(Zone zone) {
        def now = DateTime.now()
        def setTemp = Double.valueOf(zone.configurations.find { config -> config.key == CONFIG_TEMP_ALL_DAY }?.value ?: configProvider.get(Integer.class, "heat.temp.allDay"))
        Set<Configuration> tempScheduler = zone.configurations.findAll { config -> config.key == CONFIG_LIST_SCHEDULED_TEMP }.sort { it.value }

        for (Configuration config : tempScheduler) {
            def confValue = new JsonSlurper().parseText(config.value)
            def splitTime = confValue.time.split(":")
            def hour = Integer.parseInt(splitTime[0])
            def minutes = Integer.parseInt(splitTime[1])

            def scheduleTime = now.withHourOfDay(hour).withMinuteOfHour(minutes)
            if (scheduleTime.beforeNow) {
                setTemp = confValue.temp
            }
        }
        return setTemp
    }
}
