package org.myhab.services

import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import org.myhab.config.CfgKey
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.exceptions.UnavailableDeviceException

class DeviceService implements EventPublisher {

    def megaDriverService
    def espService
    def configProvider

    def readPortValuesFromDevice(Device device) throws UnavailableDeviceException {
        try {
            def values = [:]
            switch (device.model) {
                case DeviceModel.MEGAD_2561_RTC: {
                    values = megaDriverService.readPortValues(device)
                    break
                } case DeviceModel.ESP32: {
                    values = espService.readPortValues(device)
                    break
                }
            }
            if (device.status == DeviceStatus.OFFLINE) {
                publish(TopicName.EVT_DEVICE_STATUS.id(), new EventData().with {
                    p0 = TopicName.EVT_DEVICE_STATUS.id()
                    p2 = "${device.code}" // device internal code
                    p5 = "${DeviceStatus.ONLINE}" // device state
                    p6 = "http sync port values"
                    it
                })
            }
            return values
        } catch (UnavailableDeviceException unavailableDeviceException) {
            publish(TopicName.EVT_DEVICE_STATUS.id(), new EventData().with {
                p0 = TopicName.EVT_DEVICE_STATUS.id()
                p2 = "${device.code}" // device internal code
                p5 = "${DeviceStatus.OFFLINE}" // device state
                p6 = "http sync port values"
                it
            })
            throw new UnavailableDeviceException("Device[${device.id}] OFFLINE: [${unavailableDeviceException.message}]")
        } catch (Exception ex) {
            throw new UnavailableDeviceException("Read port value failed for device[${device.id}]: [${ex.message}]")
        }

    }

    @Transactional
    DevicePort importPort(Device deviceController, def portType, def portInternalRef) {
        def devicePort = DevicePort.withCriteria {
            eq('internalRef', portInternalRef)
            device {
                eq('code', deviceController.code)
            }
            maxResults(1)
        }

        if (devicePort == null && configProvider.get(Boolean.class, "admin.ports.autoimport") && deviceController.getConfigurationByKey(CfgKey.DEVICE.DEVICE_ADMIN_PORT_AUTO_IMPORT).value) {
            if (deviceController.model == DeviceModel.MEGAD_2561_RTC) {
                devicePort = megaDriverService.readPortConfigFromController(deviceController.code, portInternalRef, portInternalRef)
            } else {
                devicePort = new DevicePort()
            }
            devicePort.setType(portType)
            devicePort.setInternalRef(portInternalRef)
            deviceController.addToPorts(devicePort)
            deviceController.save(failOnError: false, flush: true)
        }
        return devicePort
    }

    Device importDevice(String deviceModel, String deviceCode) {
        Device device = Device.withCriteria {
            eq('model', DeviceModel.valueOf(deviceModel))
            eq('code', deviceCode)
            maxResults(1)
        }
        if (device == null && configProvider.get(Boolean.class, "admin.devices.autoimport")) {
            if (deviceModel == DeviceModel.MEGAD_2561_RTC) {
                device = megaDriverService.readConfig(deviceCode)
                device?.save(failOnError: false, flush: true)
            }
            if (deviceModel == DeviceModel.ESP32) {
                device = espService.readConfig(deviceCode)
                device?.save(failOnError: false, flush: true)
            }
        }
    }

    /**
     * Update device status : OFFLINE/ONLINE
     * @param event
     * @return
     */
    @Transactional
    @Subscriber('evt_device_status')
    def deviceStatus(event) {
        def device = Device.findByCode(event.data.p2)
        if (device != null && device.status != event.data.p5) {
            device.status = DeviceStatus.fromValue(event.data.p5)
            device.save(failOnError: false, flush: true)
        }
    }
}