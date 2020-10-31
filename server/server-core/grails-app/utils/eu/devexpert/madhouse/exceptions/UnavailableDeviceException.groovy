package eu.devexpert.madhouse.exceptions

import groovy.util.logging.Slf4j

@Slf4j
class UnavailableDeviceException extends RuntimeException {
    UnavailableDeviceException(String msg) {
        super(msg)
        log.error("Device read error: ${msg}")
    }
}
