package org.myhab.services

import grails.testing.services.ServiceUnitTest
import org.jsoup.Jsoup
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.exceptions.UnavailableDeviceException
import spock.lang.Specification

/**
 * readPortValues parses the raw MegaD ?cmd=all response — a single
 * semicolon-separated line covering EVERY port index in order. Relay outputs
 * report plain ON/OFF; inputs report ON|OFF/<click counter> (e.g. OFF/18),
 * which must survive verbatim for downstream ValueParser/compare logic.
 */
class MegaDriverServiceSpec extends Specification implements ServiceUnitTest<MegaDriverService> {

    // Real response captured from http://192.168.1.50/sec/?cmd=all
    static final String CMD_ALL_RESPONSE =
            'ON;ON;ON;OFF;OFF;OFF;OFF;OFF;OFF;OFF;ON;OFF;OFF;OFF;OFF;' +
            'OFF/18;OFF/6;OFF/0;OFF/7;OFF/3;OFF/1;OFF/0;' +
            'OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF;OFF'

    Device device = new Device(code: 'mega-sec', model: DeviceModel.MEGAD_2561_RTC)

    void "a full ?cmd=all response maps every port index to its raw value"() {
        given: 'the device answers ?cmd=all with a realistic all-statuses line'
            String requestedUri = null
            service.metaClass.httpClient = { Device d, String uri = null, Integer timeoutMs = null ->
                requestedUri = uri
                [readState: { Jsoup.parse("<html><body>${CMD_ALL_RESPONSE}</body></html>") }]
            }

        when:
            Map<String, String> values = service.readPortValues(device)

        then: 'the request hits the cmd=all endpoint'
            requestedUri == '?cmd=all'

        and: 'all 38 indexes are present, keyed by position'
            values.size() == 38
            values.keySet().sort { it as int } == (0..37).collect { it.toString() }

        and: 'relay outputs keep plain ON/OFF'
            values['0'] == 'ON'
            values['1'] == 'ON'
            values['10'] == 'ON'
            values['3'] == 'OFF'
            values['37'] == 'OFF'

        and: 'input ports keep their click-counter suffix verbatim'
            values['15'] == 'OFF/18'
            values['18'] == 'OFF/7'
            values['21'] == 'OFF/0'
    }

    void "an unreachable device propagates UnavailableDeviceException"() {
        given:
            service.metaClass.httpClient = { Device d, String uri = null, Integer timeoutMs = null ->
                [readState: { throw new UnavailableDeviceException('connect timed out') }]
            }

        when:
            service.readPortValues(device)

        then:
            thrown(UnavailableDeviceException)
    }
}
