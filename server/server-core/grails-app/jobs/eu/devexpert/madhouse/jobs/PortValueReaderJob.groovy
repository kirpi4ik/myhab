package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.services.EspDeviceService
import eu.devexpert.madhouse.services.MegaDriverService
import eu.devexpert.madhouse.services.PortValueService
import grails.async.Promises
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 *
 */
@Transactional
class PortValueReaderJob implements Job, EventPublisher {
    MegaDriverService megaDriverService
    EspDeviceService espDeviceService
    PortValueService portValueService

    static triggers = {
        simple name: 'portValueReader', repeatInterval: 20000
    }
    static group = "Internal"
    static description = "Read port status"

    @Override
    @Transactional
    void execute(JobExecutionContext context) throws JobExecutionException {
        Device.findAll().each { installedDevice ->
            try {
                def deviceUid = installedDevice.uid
                if (installedDevice.model.equals(DeviceModel.MEGAD_2561_RTC)) {
                    Promises.task {
                        megaDriverService.readPortValues(deviceUid)
                    }.onComplete { portValues ->
                        portValueService.updatePortValues(deviceUid, portValues)
                    }
                } else if (installedDevice.model.equals(DeviceModel.ESP8266_1)) {
                    Promises.task {
                        espDeviceService.readPortValues(deviceUid)
                    }.onComplete { portValues ->
                        portValueService.updatePortValues(deviceUid, portValues)
                    }
                }

            } catch (Exception ex) {
                log.error("Error reading port value : deviceUid=${installedDevice.uid}", ex)
            }
        }
    }


}
