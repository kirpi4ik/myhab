package eu.devexpert.madhouse.utils

import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.port.PortAction
import eu.devexpert.madhouse.exceptions.UnavailableDeviceException
import groovy.util.logging.Slf4j
import org.jsoup.HttpStatusException

import static org.apache.commons.codec.binary.Base64.encodeBase64
import static org.jsoup.Jsoup.connect

/**
 *
 */
@Slf4j
public class DeviceHttpService {
    public static final String PROTOCOL = "http"
    def DEVICE_URL_TIMEOUT = 4000
    def DEVICE_URL_TIMEOUT_ESP = 5000
    def port
    def action
    def actions = []
    def device
    def uri
    def value

    def readState() throws UnavailableDeviceException {
        def url = "[node defined]"
        if (device == null && port != null) {
            device = port.device
        }
        if (device != null && device.model == DeviceModel.MEGAD_2561_RTC) {
            url = "$PROTOCOL://${device?.networkAddress?.ip}:${device.networkAddress.port}/${device?.authAccounts?.first()?.password}/${uri != null ? uri : ''}"
            log.trace("READ STAT for [${DeviceModel.MEGAD_2561_RTC}] from : ${url}")
            try {
                return connect(url).timeout(DEVICE_URL_TIMEOUT).get()
            } catch (Exception ce) {
                throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")
            }
        } else if (device != null && device.model == DeviceModel.ESP8266_1) {
            try {
                url = "$PROTOCOL://${device.networkAddress?.ip}:${device.networkAddress?.port}/${uri != null ? uri : ''}"
                log.trace("READ STAT for [${DeviceModel.ESP8266_1}] from : ${url}")

                return connect(url)
                        .timeout(DEVICE_URL_TIMEOUT_ESP)
                        .ignoreContentType(true)
                        .header("Authorization", "Basic " + new String(encodeBase64("${device.authAccounts?.first()?.username}:${device.authAccounts?.first()?.password}".bytes)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .get()
            } catch (Exception ce) {
                throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")
            }
        } else if (device != null && device.model == DeviceModel.TMEZON_INTERCOM) {
            try {
                url = "$PROTOCOL://${device.networkAddress?.ip}:${device.networkAddress?.port}/${uri != null ? uri : ''}"
                log.trace("READ STAT for [${DeviceModel.TMEZON_INTERCOM}] from : ${url}")

                return connect(url)
                        .timeout(DEVICE_URL_TIMEOUT)
                        .ignoreContentType(true)
                        .get()
            } catch (Exception ce) {
                throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")
            }
        } else {
            log.error("Wrong device definition ${device}")
        }
    }

    def writeState() {
        def act = []

        if (device?.model == DeviceModel.MEGAD_2561_RTC || port?.device?.model == DeviceModel.MEGAD_2561_RTC) {
            if (port != null && (action != null || !actions.empty)) {
                def actionValue
                if (action != null) {
                    if (action instanceof PortAction) {
                        act << "${port.internalRef}:${action.value}"
                    } else {
                        act << "${port.internalRef}:${action}"
                    }
                }
                if (!actions.empty) {
                    actions.each {
                        if (it instanceof PortAction) {
                            act << "${port.internalRef}:${it.value}"
                        } else {
                            act << "${it}"
                        }
                    }

                }
                def url
                try {
                    url = "$PROTOCOL://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/${port.device.authAccounts.first().password}/?cmd=${act.join(";")}"

                    return connect(url).timeout(DEVICE_URL_TIMEOUT).get()
                } catch (ConnectException | HttpStatusException ce) {
                    throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")
                }
            }
        } else if (device?.model == DeviceModel.ESP8266_1 || port?.device?.model == DeviceModel.ESP8266_1) {
            if (port != null && action != null && value != null) {
                def url
                try {
                    url = "$PROTOCOL://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/cmd?action=${action}&port=${port.internalRef}&value=${value}"
                    return connect(url)
                            .ignoreContentType(true)
                            .header("Authorization", "Basic " + new String(encodeBase64("${port.device.authAccounts.first().username}:${port.device.authAccounts.first().password}".bytes)))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .get()
                } catch (ConnectException | HttpStatusException ce) {
                    throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")

                }
            }
        } else if (device?.model == DeviceModel.TMEZON_INTERCOM) {
            def url
            try {
                url = "$PROTOCOL://${device.networkAddress.ip}:${device.networkAddress.port}/${uri}"
                return connect(url)
                        .ignoreContentType(true)
                        .get()
            } catch (ConnectException | HttpStatusException ce) {
                throw new UnavailableDeviceException("Http failed for ${url}: ${ce.message}")

            }
        } else {
            log.debug("UNKNOWN DEVICE")
        }
    }
}
