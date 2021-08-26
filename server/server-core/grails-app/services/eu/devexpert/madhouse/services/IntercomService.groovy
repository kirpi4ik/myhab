package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.exceptions.PeripheralActionException
import eu.devexpert.madhouse.exceptions.UnavailableDeviceException
import eu.devexpert.madhouse.utils.DeviceHttpService
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime

@Transactional
@Slf4j
class IntercomService {

    def doorOpen(deviceId) throws UnavailableDeviceException {
        Device device = Device.findById(deviceId)
        try {
            def doorconnect = new DeviceHttpService(device: device, uri: "cgi-bin/hi3510/doorConnect.cgi?&-door=0").writeState()
        } catch (SocketException se) {
            //Unexpected end of file from server
            log.trace("Unexpected end of file from server [${se.message}]")
        }
        try {
            def doorunlock = new DeviceHttpService(device: device, uri: "web/cgi-bin/hi3510/doorUnlock.cgi?&-time=${DateTime.now().millis}").writeState()
            def resp = doorunlock?.text()
            log.debug("Door unlock")
        } catch (Exception ex) {
            throw new PeripheralActionException(ex.message)
        }
    }

    def readState(deviceUid) throws UnavailableDeviceException {
        Device deviceController = Device.findByUid(deviceUid)
        def state = new DeviceHttpService(device: deviceController, uri: "web/cgi-bin/hi3510/getnetlinknum.cgi?&-getnetlinknum&-time=${DateTime.now().millis}").readState()
    }
}
