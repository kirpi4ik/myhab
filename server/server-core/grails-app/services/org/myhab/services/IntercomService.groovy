package org.myhab.services


import org.myhab.domain.device.Device
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.utils.DeviceHttpService
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.exceptions.PeripheralActionException
import org.myhab.exceptions.UnavailableDeviceException

@Transactional
@Slf4j
class IntercomService {
    def mqttTopicService

    def doorOpen(deviceId, DevicePort port) throws UnavailableDeviceException {
        Device device = Device.findById(deviceId)
        try {
            mqttTopicService.publish(port, [PortAction.ON, "p7", PortAction.OFF])
            log.debug("Door unlock")
        } catch (Exception ex) {
            throw new PeripheralActionException(ex.message)
        }
    }

    def readState(deviceId) throws UnavailableDeviceException {
        Device deviceController = Device.findById(deviceId)
        def state = new DeviceHttpService(device: deviceController, uri: "web/cgi-bin/hi3510/getnetlinknum.cgi?&-getnetlinknum&-time=${DateTime.now().millis}").readState()
    }
}
