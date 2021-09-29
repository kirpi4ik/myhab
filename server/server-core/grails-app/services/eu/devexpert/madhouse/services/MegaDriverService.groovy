package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.port.*
import eu.devexpert.madhouse.exceptions.UnavailableDeviceException
import eu.devexpert.madhouse.utils.DeviceHttpService
import grails.events.EventPublisher
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import java.util.regex.Matcher

@Transactional
@Slf4j
class MegaDriverService implements EventPublisher {
    def dslService

    @Transactional
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
                deviceController.addToPorts(readPortFromController(deviceController.uid, portNr, portName))
            }
        }
        deviceMainPage.select("a").findAll { it.text().matches("P[0-9]{1,2}.+") }.each { portLink ->
            def portPage = new DeviceHttpService(device: deviceController, uri: portLink.attr('href')).readState()
            Matcher m = (portLink.text() =~ /P[0-9]{1,2}.+\)|P[0-9]{1,2}/)
            m.find()
            def portName = m.group(0)
            def portNr = portPage.select("form input").first().attr("value")
            deviceController.addToPorts(readPortFromController(deviceController.uid, portNr, portName))
        }


        deviceController.save(failOnError: true)

    }

    Map<String, String> readPortValues(deviceUid) throws UnavailableDeviceException {
        def response = [:]
        Device deviceController = Device.findByUid(deviceUid)
        try {
            def allStringStatus = new DeviceHttpService(device: deviceController, uri: "?cmd=all").readState()

            allStringStatus.text().split(";").eachWithIndex { status, index ->
                response << ["$index": "$status"]
            }
        } catch (Exception ex) {
            throw new UnavailableDeviceException("Read port value failed for device[$deviceUid]: [${ex.message}]")
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

    def readPortFromController(deviceUid, internalRef, portName) {
        DevicePort port
        try {
            Device deviceController = Device.findByUid(deviceUid)
            def portPage = new DeviceHttpService(device: deviceController, uri: "?pt=${internalRef}").readState()

            PortType portType = PortType.fromValue(portPage.select("form select[name=pty] option").find { option ->
                option != null && option.hasAttr("selected")
            }?.attr("value"))
            log.debug "Port ${internalRef}: $portType"

            port = new DevicePort(internalRef: internalRef, name: portName, type: portType, description: "", device: deviceController)

            port.setMiscValue(portPage.select("form input[name=misc]")?.attr("value"))
            port.setHystDeviationValue(portPage.select("form input[name=hst]")?.attr("value"))
            port.setAction(portPage.select("form input[name=ecmd]")?.attr("value"))

            switch (portType) {
                case PortType.UNKNOW:
                    break
                case PortType.IN:
                    port.setMode(PortInputMode.fromValue(portPage.select("form select[name=m] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    break
                case PortType.OUT:
                    port.setMode(PortOutputMode.fromValue(portPage.select("form select[name=m] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    port.setValue(PortOutputState.fromValue(portPage.select("form select[name=d] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    break
                case PortType.ADC:
                    port.setMode(PortAnalogicMode.fromValue(portPage.select("form select[name=m] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    break
                case PortType.DSEN:
                    port.setMode(PortSensorMode.fromValue(portPage.select("form select[name=m] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    port.setModel(PortSensorModel.fromValue(portPage.select("form select[name=d] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    break
                case PortType.I2C:
                    port.setMode(PortI2CMode.fromValue(portPage.select("form select[name=m] option").find {
                        it.hasAttr("selected")
                    }?.attr("value"))?.name())
                    break
                case PortType.NOT_CONFIGURED:
                    break
            }
        } catch (Exception ex) {
            log.error("Read port from controller", ex.message)
        }
        return port

    }


    @Subscriber('2561.input')
    def parser(event) {
        def device = Device.findByUid(event?.data?.deviceUid)
        if (event.data.pt != null) {
            def port = DevicePort.where {
                'internalRef' == event.data.pt
                'device' == device
            }.find()

            if (port != null) {
                if (port.configuration.runScenario) {
//          dSLService.execute(port.configuration.scenario)
                }
            }
        }
//    dslService.execute("""{
//      if (isEvening) {
//        lightsOn(ports: ["23734eef-8ca1-4933-9b15-60a3c539c56a"])
//      }
//    }""")


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
