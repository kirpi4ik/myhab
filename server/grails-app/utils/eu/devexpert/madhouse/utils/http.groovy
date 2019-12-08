package eu.devexpert.madhouse.common

import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortAction
import groovy.util.logging.Slf4j
import org.apache.commons.codec.binary.Base64

/**
 *
 */
@Slf4j
class http {
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
    if (device != null && device.model == DeviceModel.MEGAD_2561_RTC) {
      def url = "http://${device.networkAddress.ip}:${device.networkAddress.port}/${device.authAccounts.first().password}/${uri != null ? uri : ''}"
      log.debug("GET ${url}")
      return org.jsoup.Jsoup.connect(url).get()
    } else if (device != null && device.model == DeviceModel.ESP8266_1) {
      return org.jsoup.Jsoup.connect("http://${device.networkAddress.ip}:${device.networkAddress.port}/${uri != null ? uri : ''}")
          .timeout(2000)
          .ignoreContentType(true)
          .header("Authorization", "Basic " + new String(Base64.encodeBase64("${device.authAccounts.first().username}:${device.authAccounts.first().password}".bytes)))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .get()
    }
  }

  def call() {
    if (device?.model == DeviceModel.MEGAD_2561_RTC || port?.device.model == DeviceModel.MEGAD_2561_RTC) {
      if (port != null && action != null) {
        def actionValue
        if (action instanceof PortAction) {
          actionValue = action.value
        } else {
          actionValue = action
        }
//        println "GET http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/${port.device.authAccounts.first().password}/?cmd=${port.internalRef}:${actionValue}"
        try {
          return org.jsoup.Jsoup.connect("http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/${port.device.authAccounts.first().password}/?cmd=${port.internalRef}:${actionValue}").timeout(2000).get()
        } catch (ConnectException ce) {
          log.error(ce)
        }
      }
    } else if (device?.model == DeviceModel.ESP8266_1 || port?.device.model == DeviceModel.ESP8266_1) {
      if (port != null && action != null && value != null) {
        try {
//          println "GET http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/cmd?action=${action}&port=${port.internalRef}&value=${value}"
          return org.jsoup.Jsoup.connect("http://${port.device.networkAddress.ip}:${port.device.networkAddress.port}/cmd?action=${action}&port=${port.internalRef}&value=${value}")
              .ignoreContentType(true)
              .header("Authorization", "Basic " + new String(Base64.encodeBase64("${port.device.authAccounts.first().username}:${port.device.authAccounts.first().password}".bytes)))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .get()
        } catch (ConnectException ce) {
          log.error(ce)
        }
      }
    }else{
      log.debug( "UNKNOWN DEVICE")
    }
  }

  void setUri(uri) {
    this.uri = uri
  }

  void setDevice(device) {
    this.device = device
  }

  void setPort(DevicePort port) {
    this.port = port
  }

  void setAction(action) {
    this.action = action
  }

  void setValue(value) {
    this.value = value
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
