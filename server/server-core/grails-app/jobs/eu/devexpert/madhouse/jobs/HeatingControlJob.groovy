package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.infra.Zone
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
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
        simple name: 'heatControlJob', repeatInterval: TimeUnit.MINUTES.toMillis(20)
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
            def tempValue = Double.valueOf(peripheral.configurations.find { config -> config.key = "key.temp.allDay.value" }?.value ?: tempAllDay)
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
                        if (Double.valueOf(termostatPort.value) <= tempValue) {
                            println("[${termostatPort.value} < ${tempValue}] | HEAT STARTING : ${leafZone.name}")
                            heatService.heatOn(peripheral)
                        } else {
                            log.debug("[${termostatPort.value} > ${tempValue}] | HEAT CLOSING: ${leafZone.name}")
                            heatService.heatOff(peripheral)
                        }
                    }
                }
            }
        }
    }
}
