package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.utils.http
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import grails.gorm.transactions.Transactional

@Transactional
@Slf4j
class EspDeviceService {
    @Transactional
    def readConfig() {

    }

    Map<String, String> readPortValues(deviceUid) {
        def response = [:]
        Device deviceController = Device.findByUid(deviceUid)
        def portStatusJson = http.url {
            device = deviceController;
            uri = "cmd?action=get&port=ALL"
        }
        def resp = new JsonSlurper().parseText("${portStatusJson?.text()}")

        resp?.ports?.each {
            response << ["${it.nr}": "${it.status}"]
        }

        return response
    }

    def setValue(devicePort, newValue) {
        http.get {
            device = devicePort.device;
            port = devicePort
            action = "set"
            value = newValue
        }
    }
}