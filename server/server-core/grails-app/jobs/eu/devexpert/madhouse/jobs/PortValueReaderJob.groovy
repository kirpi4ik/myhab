package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.DeviceStatus
import eu.devexpert.madhouse.exceptions.UnavailableDeviceException
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
        simple name: 'portValueReader', repeatInterval: 6000
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
                        try {
                            megaDriverService.readPortValues(deviceUid)
                        } catch (UnavailableDeviceException ex) {
                            publish(TopicName.EVT_DEVICE_STATUS.id(), [
                                    "p0": TopicName.EVT_DEVICE_STATUS.id(),
                                    "p1": EntityType.DEVICE.name(),
                                    "p2": installedDevice?.uid,
                                    "p3": "read_device_controller_status",
                                    "p4": DeviceStatus.OFFLINE,
                                    "p6": "system"
                            ])
                            throw new UnavailableDeviceException()
                        }
                    }.onComplete { portValues ->
                        portValueService.updateIfChangedPortValues(deviceUid, portValues)
                        publish(TopicName.EVT_DEVICE_STATUS.id(), [
                                "p0": TopicName.EVT_DEVICE_STATUS.id(),
                                "p1": EntityType.DEVICE.name(),
                                "p2": installedDevice?.uid,
                                "p3": "read_device_controller_status",
                                "p4": DeviceStatus.ONLINE,
                                "p6": "system"
                        ])
                    }.onError { Throwable t ->
                        log.error("onError reading port value : deviceUid=${installedDevice.uid}")
                    }
                } else if (installedDevice.model.equals(DeviceModel.ESP8266_1)) {
                    Promises.task {
                        try {
                            espDeviceService.readPortValues(deviceUid)
                        } catch (UnavailableDeviceException ex) {
                            publish(TopicName.EVT_DEVICE_STATUS.id(), [
                                    "p0": TopicName.EVT_DEVICE_STATUS.id(),
                                    "p1": EntityType.DEVICE.name(),
                                    "p2": installedDevice?.uid,
                                    "p3": "read_device_controller_status",
                                    "p4": DeviceStatus.OFFLINE,
                                    "p6": "system"
                            ])
                            throw new UnavailableDeviceException()
                        }
                    }.onComplete { portValues ->
                        portValueService.updateIfChangedPortValues(deviceUid, portValues)
                        publish(TopicName.EVT_DEVICE_STATUS.id(), [
                                "p0": TopicName.EVT_DEVICE_STATUS.id(),
                                "p1": EntityType.DEVICE.name(),
                                "p2": installedDevice?.uid,
                                "p3": "read_device_controller_status",
                                "p4": DeviceStatus.ONLINE,
                                "p6": "system"
                        ])
                    }.onError { Throwable t ->
                        log.error("onError reading port value : deviceUid=${installedDevice.uid}")
                    }
                }

            } catch (UnavailableDeviceException ex) {
                publish(TopicName.EVT_DEVICE_STATUS.id(), [
                        "p0": TopicName.EVT_DEVICE_STATUS.id(),
                        "p1": EntityType.DEVICE.name(),
                        "p2": installedDevice?.uid,
                        "p3": "read_device_controller_status",
                        "p4": DeviceStatus.OFFLINE,
                        "p6": "system"
                ])
                log.error("Error reading port value : deviceUid=${installedDevice.uid}", ex)
            }
        }
    }


}
