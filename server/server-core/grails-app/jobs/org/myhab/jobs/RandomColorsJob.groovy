package org.myhab.jobs


import grails.events.EventPublisher
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.RandomUtils
import org.myhab.ConfigKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.DevicePeripheral
import org.myhab.utils.DeviceHttpService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@Slf4j
class RandomColorsJob implements Job, EventPublisher {

    static triggers = {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.randomColors.enabled', Boolean)
        def interval = config?.getProperty('quartz.jobs.randomColors.interval', Integer) ?: 5
        
        if (enabled == null) {
            enabled = false  // Default to disabled (demo/testing job)
        }
        
        if (enabled) {
            println "RandomColorsJob: ENABLED - Registering trigger with interval ${interval}s"
            simple name: 'randomColors', repeatInterval: TimeUnit.SECONDS.toMillis(interval)
        } else {
            println "RandomColorsJob: DISABLED - Not registering trigger"
        }
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        def enabled = config?.getProperty('quartz.jobs.randomColors.enabled', Boolean)
        
        if (enabled == null) {
            enabled = false
        }
        
        if (!enabled) {
            log.info("RandomColorsJob is DISABLED via configuration, skipping execution")
            return
        }
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
