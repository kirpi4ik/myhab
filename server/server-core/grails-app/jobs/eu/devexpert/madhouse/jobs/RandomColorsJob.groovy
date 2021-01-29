package eu.devexpert.madhouse.jobs


import eu.devexpert.madhouse.ConfigKey
import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.utils.DeviceHttpService
import grails.events.EventPublisher
import org.apache.commons.lang3.RandomUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

class RandomColorsJob implements Job, EventPublisher {

    static triggers = {
        simple name: 'randomColors', repeatInterval: TimeUnit.SECONDS.toMillis(5000)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        Collection<DevicePeripheral> randomPeripherals = DevicePeripheral.findAll { p ->
            Configuration.where {
                entityId == p.id && entityType == EntityType.PERIPHERAL.name() && key == ConfigKey.CONFIG_LIGHT_RGB_RANDOM && value == "true"
            }[0] != null
        }

        randomPeripherals.each { peripheral ->
            peripheral.connectedTo.each { port ->
                def rgbConf = Configuration.where {
                    entityId == port.id && entityType == EntityType.PORT.name() && key == ConfigKey.CONFIG_LIGHT_RGB_COLOR
                }[0]
                if (rgbConf != null) {
                    new DeviceHttpService(port: port, action: RandomUtils.nextInt(50, 280)).writeState()
                }
            }
        }
    }
}
