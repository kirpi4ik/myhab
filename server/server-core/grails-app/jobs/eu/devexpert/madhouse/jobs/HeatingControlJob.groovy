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
        simple name: 'heatControlJob', repeatInterval: TimeUnit.MINUTES.toMillis(1)
    }
    public static final String PERIPHERAL_HEAT_CTRL_CATEGORY = "HEAT"
    public static final String PERIPHERAL_TEMPERATURE_SENSOR_CATEGORY = 'TEMP'
    def heatService
    def tempAllDay = 21

    static group = "Internal"
    static description = "Heat control"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        Set<DevicePeripheral> actuatorHeatCtrlSet = DevicePeripheral.where {
            category.name == PERIPHERAL_HEAT_CTRL_CATEGORY
        }.findAll()

        actuatorHeatCtrlSet.each { actuator ->

            def desiredTempForActuator = getDesiredTemperature(actuator)

            Set<Zone> leafZones = []
            def belongsToOrphanZones = actuator.zones.findAll { zone -> zone.zones.isEmpty() }
//            actuator.zones.each { leafZones << allChildZones(it) }
            belongsToOrphanZones.each { leafZone ->
                DevicePeripheral.createCriteria().list {
                    category {
                        eq('name', PERIPHERAL_TEMPERATURE_SENSOR_CATEGORY)
                    }
                    zones {
                        idEq(leafZone.id)
                    }
                }.each { deviceThermoPeripheral ->
                    deviceThermoPeripheral.connectedTo.findAll { termostatPort -> termostatPort.value?.isNumber() }.each { termostatPort ->
                        if (Double.valueOf(termostatPort.value) <= desiredTempForActuator) {
                            println("[${termostatPort.value} < ${desiredTempForActuator}] | HEAT STARTING : ${leafZone.name}")
                            heatService.heatOn(actuator)
                        } else {
                            log.debug("[${termostatPort.value} > ${desiredTempForActuator}] | HEAT CLOSING: ${leafZone.name}")
                            heatService.heatOff(actuator)
                        }
                    }
                }
            }
        }
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

    def getDesiredTemperature(DevicePeripheral peripheral) {
        def now = DateTime.now()
        def setTemp = Double.valueOf(peripheral.configurations.find { config -> config.key == "key.temp.allDay.value" }?.value ?: tempAllDay)
        Set<Configuration> tempScheduler = peripheral.configurations.findAll { config -> config.key == "key.temp.schedule.list.value" }.sort { it.value }

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
