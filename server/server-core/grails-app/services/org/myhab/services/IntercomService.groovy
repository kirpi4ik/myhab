package org.myhab.services


import org.myhab.domain.device.Device
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortAction
import org.myhab.utils.DeviceHttpService
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.myhab.exceptions.PeripheralActionException
import org.myhab.exceptions.UnavailableDeviceException

@Transactional
@Slf4j
class IntercomService {
    def mqttTopicService

    private static final int HTTP_TIMEOUT_MS = 4000

    def doorOpen(deviceId, DevicePort port) throws UnavailableDeviceException {
        Device device = Device.findById(deviceId)
        try {
            mqttTopicService.publish(port, [PortAction.ON, "p7", PortAction.OFF])
            log.debug("Door unlock")
        } catch (Exception ex) {
            throw new PeripheralActionException(ex.message)
        }
    }

    /**
     * Unlock a TMEZON intercom over its own hi3510 CGI: activate the session
     * with doorConnect (this firmware answers by closing the socket — expected),
     * then doorUnlock. An optional checkuser login runs first only if the device
     * carries web credentials; the stock ipcam/w54723 are stream-only and
     * checkuser rejects them, so unlock relies on the firmware's open CGI.
     *
     * <p>Kept entirely separate from {@link #doorOpen}, the MegaD-relay gate —
     * no shared path, no fallback between them.</p>
     */
    def unlockIntercom(Device device) throws UnavailableDeviceException {
        if (device?.networkAddress?.ip == null) {
            throw new PeripheralActionException("intercom device has no network address")
        }
        String base = "http://${device.networkAddress.ip}:${device.networkAddress.port ?: 80}"
        Map<String, String> cookies = optionalLogin(device, base)
        try {
            // Activate the session. This firmware aborts the socket instead of
            // replying; that is the success signal, not a failure.
            try {
                Jsoup.connect("${base}/web/cgi-bin/hi3510/doorConnect.cgi?&-door=0")
                        .header("Referer", "${base}/web/index.html#")
                        .cookies(cookies).ignoreContentType(true).ignoreHttpErrors(true)
                        .timeout(HTTP_TIMEOUT_MS).method(Connection.Method.GET).execute()
            } catch (SocketException | SocketTimeoutException expected) {
                log.debug("doorConnect closed the socket (expected): ${expected.message}")
            }
            Jsoup.connect("${base}/web/cgi-bin/hi3510/doorUnlock.cgi?&-time=${DateTime.now().millis}")
                    .cookies(cookies).ignoreContentType(true).ignoreHttpErrors(true)
                    .timeout(HTTP_TIMEOUT_MS).method(Connection.Method.GET).execute()
            log.info("Intercom ${device.code} unlocked")
        } catch (Exception ex) {
            throw new PeripheralActionException("intercom unlock failed: ${ex.message}")
        }
    }

    private Map<String, String> optionalLogin(Device device, String base) {
        def account = device.authAccounts?.find()
        if (!account?.username || !account?.password) {
            return [:]
        }
        try {
            String ts = DateTimeFormat.forPattern("yyyy.MM.dd.HH.mm.ss").print(DateTime.now())
            def resp = Jsoup.connect("${base}/cgi-bin/hi3510/checkuser.cgi?&-name=${account.username}&-passwd=${account.password}&-time=${ts}&-timezone=0")
                    .ignoreContentType(true).ignoreHttpErrors(true)
                    .timeout(HTTP_TIMEOUT_MS).method(Connection.Method.GET).execute()
            return resp.cookies() ?: [:]
        } catch (Exception ignored) {
            log.debug("intercom checkuser login skipped: ${ignored.message}")
            return [:]
        }
    }

    def readState(deviceId) throws UnavailableDeviceException {
        Device deviceController = Device.findById(deviceId)
        def state = new DeviceHttpService(device: deviceController, uri: "web/cgi-bin/hi3510/getnetlinknum.cgi?&-getnetlinknum&-time=${DateTime.now().millis}").readState()
    }
}
