package eu.devexpert.madhouse.jobs

import eu.devexpert.madhouse.domain.TopicName
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DeviceModel
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.domain.job.EventData
import eu.devexpert.madhouse.parser.ValueParser
import eu.devexpert.madhouse.services.EspDeviceService
import eu.devexpert.madhouse.services.MegaDriverService
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.joda.time.DateTime
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 *
 */
@Transactional
class PortValueReaderJob implements Job, EventPublisher {
  MegaDriverService megaDriverService
  EspDeviceService espDeviceService
  static triggers = {
    simple name: 'portValueReader', repeatInterval: 5000
  }
  static group = "Internal"
  static description = "Read port status"

  @Override
  @Transactional
  void execute(JobExecutionContext context) throws JobExecutionException {
    Device.findAll().each { installedDevice ->
      def deviceUid = installedDevice.uid
      if (installedDevice.model.equals(DeviceModel.MEGAD_2561_RTC)) {
        updatePortValues(deviceUid, megaDriverService.readPortValues(deviceUid))
      } else if (installedDevice.model.equals(DeviceModel.ESP8266_1)) {
        updatePortValues(deviceUid, espDeviceService.readPortValues(deviceUid))
      }
    }
  }

  def updatePortValues(deviceUid, portValues) {
    portValues.each { portRef, newValue ->
      def devicePorts = DevicePort.withCriteria {
        eq('internalRef', portRef)
        device {
          eq('uid', deviceUid)
        }
        maxResults(1)
      }
      if (devicePorts.size() > 0) {
        //Search latest value if any
        def devicePort = devicePorts[0]
        def portLatestValues = PortValue.withCriteria {
          eq('portUid', devicePort.uid)
          order("tsCreated", "desc")
          maxResults(1)
        }
        if (devicePort.type.syncMs != -1) {
          if (portLatestValues.size() == 0 || DateTime.now().minus(portLatestValues[0]?.tsCreated?.time).isAfter(devicePort?.type?.syncMs))
            if (devicePort.value == null || !devicePort.value.equalsIgnoreCase(ValueParser.parser(devicePort).apply(newValue))) {
              EventData eventData = new EventData().with {
                p0 = TopicName.PORT_VALUE_CHANGE.id()
                p1 = "PORT"
                p2 = "${devicePort.uid}"
                p3 = "cron"
                p4 = "$newValue"
                it
              }
              publish(eventData.p0, eventData)
            }
        } else {
          //skip event
        }
      }
    }
  }
}
