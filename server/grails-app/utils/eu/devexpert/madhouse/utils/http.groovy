package eu.devexpert.madhouse.utils

import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.port.PortAction
import groovy.util.logging.Slf4j
import org.jsoup.HttpStatusException

import static org.apache.commons.codec.binary.Base64.encodeBase64
import static org.jsoup.Jsoup.connect

/**
 *
 */
@Slf4j
class http {
    def DEVICE_URL_TIMEOUT = 2000
    def port
    def action
    def device
    def uri
    def value


    def static get(closure) {
        closure.delegate = new http()
        closure()
        closure.getDelegate().call()
    }

    def static url(closure) {
        closure.delegate = new http()
        closure()
        closure.getDelegate().url()
    }

    def url() {
        def url = "[node defined]"
        if (device != null && device.model == DeviceModel.MEGAD_2561_RTC) {
            url = "http://${device?.networkAddress?.ip}:${device.networkAddress.port}/${device?.authAccounts?.first()?.password}/${uri != null ? uri : ''}"
            log.debug("READ STAT for [${DeviceModel.MEGAD_2561_RTC}] from : ${url}")
            try {
                return connect(url).get()
            } catch (Exception ce) {
                log.error("Http failed for ${url}: ${ce.message}")
            }
        } else if (device != null && device.model == DeviceModel.ESP8266_1) {
            try {
                url = "http://${device.networkAddress?.ip}:${device.networkAddress?.port}/${uri != null ? uri : ''}"
                log.debug("READ STAT for [${DeviceModel.ESP8266_1}] from ${url}")
                return connect(url)
                        .timeout(DEVICE_URL_TIMEOUT)
                        .ignoreContentType(true)
                        .header("Authorization", "Basic " + new String(encodeBase64("${device.authAccounts?.first()?.username}:${device.authAccounts?.first()?.password}".bytes)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .get()
            } catch (Exception ce) {
                log.error("Http failed for ${url}: ${ce.message}")
            }
        } else {
            log.error("Wrong device definition ${device}")
        }
    }

    def call() {
        if (device?.model == DeviceModel.MEGAD_2561_RTC || port?.device?.model == DeviceModel.MEGAD_2561_RTC) {
            if (port != null && action != null) {
                def actionValue
                if (action instanceof PortAction) {
                    actionValue = action.value
                } else {
                    actionValue = action
                }
                def url
                try {
                    url = "http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/${port.device.authAccounts.first().password}/?cmd=${port.internalRef}:${actionValue}"
                    return connect(url).timeout(DEVICE_URL_TIMEOUT).get()
                } catch (ConnectException | HttpStatusException ce) {
                    log.error("Http failed for ${url}: ${ce.message}")
                }
            }
        } else if (device?.model == DeviceModel.ESP8266_1 || port?.device?.model == DeviceModel.ESP8266_1) {
            if (port != null && action != null && value != null) {
                def url
                try {
                    url = "http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/cmd?action=${action}&port=${port.internalRef}&value=${value}"
                    return connect(url)
                            .ignoreContentType(true)
                            .header("Authorization", "Basic " + new String(encodeBase64("${port.device.authAccounts.first().username}:${port.device.authAccounts.first().password}".bytes)))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .get()
                } catch (ConnectException | HttpStatusException ce) {
                    log.error("Http failed for ${url}: ${ce.message}")

                }
            }
        } else {
            log.debug("UNKNOWN DEVICE")
        }
    }

    def methodMissing(String methodName, args) {
        log.error("missing method : ${methodName}")
//    return new eu.devexpert.madhouse.dsl.action.PowerOut(args)
    }

    def propertyMissing(String paramName) {
        log.error("missing property: ${paramName}")
//    return new eu.devexpert.madhouse.dsl.action.PowerOut()
    }

}
