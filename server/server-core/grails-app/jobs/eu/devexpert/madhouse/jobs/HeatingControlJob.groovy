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
        simple name: 'heatControlJob', repeatInterval: TimeUnit.MINUTES.toMillis(10)
    }
    def heatService
    def tempAllDay = 21

    static group = "Internal"
    static description = "Heat control"

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        Set<DevicePeripheral> peripheralsWithHeatCircuit = DevicePeripheral.where {
            category.name == "HEAT"
        }.findAll()

        peripheralsWithHeatCircuit.each { peripheral ->
            def now = DateTime.now()
            def allDayTempValue = Double.valueOf(peripheral.configurations.find { config -> config.key == "key.temp.allDay.value" }?.value ?: tempAllDay)
            def setTemp = allDayTempValue
            Set<Configuration> tempScheduler = peripheral.configurations.findAll { config -> config.key == "key.temp.schedule.list.value" }.sort{it.value}

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


            Set<Zone> leafZones = peripheral.zones.findAll { zone -> zone.zones.empty }
            leafZones.each { leafZone ->
                DevicePeripheral.createCriteria().list {
                    category {
                        eq('name', 'TEMP')
                    }
                    zones {
                        idEq(leafZone.id)
                    }
                }.each { termostat ->
                    termostat.connectedTo.findAll { termostatPort -> termostatPort.value?.isNumber() }.each { termostatPort ->
                        if (Double.valueOf(termostatPort.value) <= setTemp) {
                            println("[${termostatPort.value} < ${setTemp}] | HEAT STARTING : ${leafZone.name}")
                            heatService.heatOn(peripheral)
                        } else {
                            log.debug("[${termostatPort.value} > ${setTemp}] | HEAT CLOSING: ${leafZone.name}")
                            heatService.heatOff(peripheral)
                        }
                    }
                }
            }
        }
    }
}
