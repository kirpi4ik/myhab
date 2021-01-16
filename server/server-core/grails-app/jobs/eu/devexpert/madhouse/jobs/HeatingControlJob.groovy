package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.infra.Zone
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

/**
 *
 */
@Slf4j
@Transactional
class HeatingControlJob implements Job, EventPublisher {
    static triggers = {
        simple name: 'heatControlJob', repeatInterval: TimeUnit.MINUTES.toMillis(3)
    }
    public static final String PERIPHERAL_HEAT_CTRL_CATEGORY = "HEAT"
    public static final String PERIPHERAL_TEMPERATURE_SENSOR_CATEGORY = 'TEMP'
    public static final String CONFIG_LIST_SCHEDULED_TEMP = "key.temp.schedule.list.value"
    def heatService
    def tempAllDay = 21

    static group = "Internal"
    static description = "Heat control"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def actions = [:]
        Set<Zone> allzones = Zone.findAll()
        def zonesWithScheduledTemp = allzones.findAll { zone -> zone.getConfigurations().find { configuration -> configuration.key == CONFIG_LIST_SCHEDULED_TEMP } != null }.sort { zone -> zone.parent }.reverse()
        zonesWithScheduledTemp.each { zone ->
            def desiredTempForActuator = getDesiredTemperature(zone)
            def currentTemperatures = getCurrentTempSInZone(zone)
            if (desiredTempForActuator != null && currentTemperatures.size() > 0) {
                //there is at least one thermo sensor
                def currentTemp = currentTemperatures[0]

                Set<DevicePeripheral> actuatorHeatCtrlSet = DevicePeripheral.findAll("select dp from  DevicePeripheral dp where ?0 in elements(zones) and dp.category.name = ?1", [zone], PERIPHERAL_HEAT_CTRL_CATEGORY)
                actuatorHeatCtrlSet.each { actuator ->
                    if (currentTemp <= desiredTempForActuator) {
                        actions << ["${actuator.id}": "ON"]

                    } else {
                        actions << ["${actuator.id}": "OFF"]

                    }
                }

            }
        }
        actions.each { actuatorId, action ->
            def actuator = DevicePeripheral.findById(Long.valueOf(actuatorId))
            if (action == "ON") {
                log.debug("HEAT STARTING : ${actuator.name}")
                heatService.heatOn(actuator)
            } else if (action == "OFF") {
                log.debug("HEAT CLOSING: ${actuator.name}")
                heatService.heatOff(actuator)
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
        def setTemp = Double.valueOf(zone.configurations.find { config -> config.key == "key.temp.allDay.value" }?.value ?: tempAllDay)
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
