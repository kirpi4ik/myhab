package eu.devexpert.madhouse.parser

import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortType

import java.util.function.Function

/**
 *
 */
class ValueParser {

  static Function<String, String> parser(DevicePort devicePort) {
    switch (devicePort.type) {
      case PortType.DSEN: return { value ->
        if (value.startsWith("temp:")) {
          return value.substring(5)
        } else {
          return value
        }
      };
      default: return { value -> value }
    }
  }
}
