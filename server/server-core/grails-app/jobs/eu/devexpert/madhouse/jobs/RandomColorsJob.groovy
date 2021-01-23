package eu.devexpert.madhouse.jobs

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.core.IMap
import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.init.cache.CacheMap
import eu.devexpert.madhouse.utils.DeviceHttpService
import grails.events.EventPublisher
import org.apache.commons.lang3.RandomUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

class RandomColorsJob implements Job, EventPublisher {
    HazelcastInstance hazelcastInstance;

    static triggers = {
        simple name: 'randomColors', repeatInterval: TimeUnit.SECONDS.toMillis(5)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
//        def map = hazelcastInstance.getMap(CacheMap.EXPIRE)
//        def red = map.get("r")
//        def green = map.get("g")
//        def blue = map.get("b")
        def b = DevicePort.findById(22632)
        def r = DevicePort.findById(22633)
        def g = DevicePort.findById(22634)


        def periph = b.peripherals.findAll { p ->
            Configuration.where {
                entityId == p.id && entityType == "PERIPHERAL" && key == "key.light.rgbRandom" && value == "true"
            }[0] != null
        }
        if (!periph.empty) {
            new DeviceHttpService(port: r, action: RandomUtils.nextInt(0, 255)).writeState()
            new DeviceHttpService(port: g, action: RandomUtils.nextInt(0, 255)).writeState()
            new DeviceHttpService(port: b, action: RandomUtils.nextInt(0, 255)).writeState()
        }
    }
}
