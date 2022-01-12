package org.myhab.services


import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional

@Transactional
class DeviceService {

    def megaDriverService
    def espService
    def configProvider

    DevicePort importPort(Device deviceController, def portType, def portCode) {
        def devicePort = DevicePort.withCriteria {
            eq('internalRef', portCode)
            device {
                eq('code', deviceController.code)
            }
            maxResults(1)
        }

        if (devicePort == null && configProvider.get(Boolean.class, "admin.ports.autoimport")) {
            if (device.model.equals(DeviceModel.MEGAD_2561_RTC)) {
                devicePort = megaDriverService.readPortConfigFromController(device.code, portCode, portCode)
                device.addToPorts(devicePort)
                device.save(failOnError: false, flush: true)
            }
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
            if (deviceModel.equals(DeviceModel.MEGAD_2561_RTC)) {
                device = megaDriverService.readConfig(deviceCode)
                device?.save(failOnError: false, flush: true)
            }
            if (deviceModel.equals(DeviceModel.ESP8266_1)) {
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
        if (device.status != event.data.p5) {
            device.status = DeviceStatus.fromValue(event.data.p5)
            device.save(failOnError: false, flush: true)
        }
    }
}
