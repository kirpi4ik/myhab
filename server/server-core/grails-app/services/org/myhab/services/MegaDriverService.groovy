package org.myhab.services

import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import groovy.util.logging.Slf4j
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortType
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.utils.DeviceHttpService

import java.util.regex.Matcher

@Slf4j
class MegaDriverService implements EventPublisher {
    def dslService

    def readConfig(String deviceCode) {

    }

    def readConfig(Device deviceController) {
        def deviceMainPage = new DeviceHttpService(device: deviceController).readState()

        def configUrl = deviceMainPage.select("a").find { it.text() == "Config" }.attr("href")
        deviceMainPage.select("a").findAll { it.text().matches("XP[0-9]+") }.each { link ->
            def portListPage = new DeviceHttpService(device: deviceController, uri: link.attr('href')).readState()
            portListPage.select("a").findAll {
                it.text().matches("P[0-9]{1,2}.+")
            }.each { portLink ->
                def portPage = new DeviceHttpService(device: deviceController, uri: portLink.attr('href')).readState()
                Matcher m = (portLink.text() =~ /P[0-9]{1,2}.+\)|P[0-9]{1,2}/)
                m.find()
                def portName = m.group(0)
                def portNr = portPage.select("form input").first().attr("value")
                deviceController.addToPorts(readPortConfigFromController(deviceController.code, portNr, portName))
            }
        }
        deviceMainPage.select("a").findAll { it.text().matches("P[0-9]{1,2}.+") }.each { portLink ->
            def portPage = new DeviceHttpService(device: deviceController, uri: portLink.attr('href')).readState()
            Matcher m = (portLink.text() =~ /P[0-9]{1,2}.+\)|P[0-9]{1,2}/)
            m.find()
            def portName = m.group(0)
            def portNr = portPage.select("form input").first().attr("value")
            deviceController.addToPorts(readPortConfigFromController(deviceController.code, portNr, portName))
        }


        deviceController.save(failOnError: true)

    }

    Map<String, String> readPortValues(deviceController) throws UnavailableDeviceException {
        def response = [:]
            def allStringStatus = new DeviceHttpService(device: deviceController, uri: "?cmd=all").readState()
            allStringStatus.text().split(";").eachWithIndex { status, index -> response << ["$index": "$status"]
            }
        return response
    }

    def readPortValue(deviceUid, portNr) {
        try {
            Device deviceController = Device.findByUid(deviceUid)
            return new DeviceHttpService(device: deviceController, uri: "?pt=${portNr}&cmd=get").readState()
        } catch (Exception ex) {
            log.error("Read port value failed deviceUid=[$deviceUid], portNr=[$portNr]", ex.message)
        }
    }

    def readPortConfigFromController(code, internalRef, portName) {
        DevicePort port
        try {
            Device deviceController = Device.findByCode(code)
            def portPage = new DeviceHttpService(device: deviceController, uri: "?pt=${internalRef}").readState()

            PortType portType = PortType.fromValue(portPage.select("form select[name=pty] option").find { option -> option != null && option.hasAttr("selected")
            }?.attr("value"))

            port = new DevicePort(internalRef: internalRef, name: portName, type: portType, description: "", device: deviceController)

            port.configurations << new Configuration(entityType: EntityType.PORT, key: "cfg.key.port.portType", portPage.select("form select[name=pty] option")?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.miscValue", portPage.select("form input[name=misc]")?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.HystDeviationValue", portPage.select("form input[name=hst]")?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.action", portPage.select("form input[name=ecmd]")?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.mode", portPage.select("form select[name=m] option").find { it.hasAttr("selected") }?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.outputState", portPage.select("form select[name=d] option").find { it.hasAttr("selected") }?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.analogMode", portPage.select("form select[name=m] option").find { it.hasAttr("selected") }?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.sensorMode", portPage.select("form select[name=m] option").find { it.hasAttr("selected") }?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.sensorModel", portPage.select("form select[name=d] option").find { it.hasAttr("selected") }?.attr("value"))
            port.configurations << new Configuration(entityType: EntityType.PORT, "cfg.key.port.i2cMode", portPage.select("form select[name=m] option").find { it.hasAttr("selected") }?.attr("value"))

        } catch (Exception ex) {
            log.error("Read port from controller", ex.message)
        }
        return port

    }


    @Subscriber('2561.run.action')
    def runAction(event) {
        log.debug("call action")
        new DeviceHttpService(device: Device.findById(event?.data?.deviceUid), uri: '?cmd=' + event?.data?.actionBody).readState()

    }

    @Subscriber('2561.run.scenario')
    def runScenario(event) {
        dslService.execute(event?.data?.scenarioBody)
    }

}
