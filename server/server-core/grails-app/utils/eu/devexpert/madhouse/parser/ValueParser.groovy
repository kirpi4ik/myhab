package eu.devexpert.madhouse.parser

import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortType

import java.util.function.Function

/**
 *
 */
class ValueParser {

    static Function<String, String> parser(DevicePort devicePort) {
        def matcher
        def regex;
        switch (devicePort.type) {
            case PortType.DSEN: return { value ->
                regex = "temp:(\\d{1,2})"
                if (value.matches(regex)) {
                    matcher = value =~ regex
                    return matcher[0][1]
                } else {
                    return value
                }
            };
            case PortType.ADC: return { value ->
                regex = "P(\\d{1,2})/(\\d{1,4})"
                if (value.matches(regex)) {
                    matcher = value =~ regex
                    return matcher[0][2]
                } else {
                    return value
                }
            };
            default: return { value -> value }
        }
    }
}
