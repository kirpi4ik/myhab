package org.myhab.services.dsl.knowledge

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonOutput
import org.myhab.config.ConfigProvider
import org.myhab.domain.device.Device
import org.myhab.domain.device.port.DevicePort
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Verifies the recent-rain predicates {@code rainDuration} / {@code rainAmount},
 * which read the meteo {@code hourly.precipitation} / {@code hourly.time} arrays.
 *
 * The meteo device is supplied by stubbing {@code Device.get(...)} so no GORM
 * persistence is needed. Hourly timestamps are built relative to "now" in GMT
 * (matching the timezone port) so the window math is deterministic; assertions
 * use small tolerances to absorb the sub-second gap between building the
 * fixtures and the service reading the clock.
 */
class KnowledgeServiceSpec extends Specification implements ServiceUnitTest<KnowledgeService>, DataTest {

    Class[] getDomainClassesToMock() { [Device, DevicePort] as Class[] }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private static final ZoneId GMT = ZoneId.of('GMT')

    def setup() {
        service.configProvider = Mock(ConfigProvider) {
            get(Long.class, KnowledgeService.CFG_METEO_DEVICE_ID) >> 1L
            get(Double.class, KnowledgeService.CFG_RAIN_THRESHOLD) >> 0.0d
            get(Long.class, KnowledgeService.CFG_RAIN_PERIPHERAL_ID) >> null
        }
    }

    def cleanup() {
        GroovySystem.metaClassRegistry.removeMetaClass(Device)
    }

    /** Stub Device.get to return a meteo device carrying the given ports. */
    private void stubMeteoDevice(Map<String, String> portsByRef) {
        Device device = new Device()
        device.ports = portsByRef.collect { ref, val -> new DevicePort(internalRef: ref, value: val) } as Set
        Device.metaClass.static.get = { Long id -> id == 1L ? device : null }
    }

    /** ISO-local timestamp for (now + offsetHours) in GMT. */
    private static String slot(int offsetHours) {
        return ZonedDateTime.now(GMT).toLocalDateTime().plusHours(offsetHours).format(FMT)
    }

    void "rainDuration and rainAmount return 0 when meteo data is unavailable"() {
        given:
            stubMeteoDevice([:])

        expect:
            service.rainDuration(120) == 0
            service.rainAmount(120) == 0.0d
    }

    void "non-positive lookback yields 0"() {
        given:
            stubMeteoDevice([
                'timezone'            : 'GMT',
                'hourly.time'         : JsonOutput.toJson([slot(-1), slot(0)]),
                'hourly.precipitation': JsonOutput.toJson([1.0, 2.0]),
            ])

        expect:
            service.rainDuration(0) == 0
            service.rainDuration(-30) == 0
            service.rainAmount(0) == 0.0d
    }

    void "rain in the past hours within the window counts duration and amount"() {
        given:
            // Preceding-hour slots ending at now-1h and now both fall inside (now-2h, now].
            // The now-2h slot is on the window boundary (excluded); now+1h is in the future (excluded).
            stubMeteoDevice([
                'timezone'            : 'GMT',
                'hourly.time'         : JsonOutput.toJson([slot(-2), slot(-1), slot(0), slot(1)]),
                'hourly.precipitation': JsonOutput.toJson([5.0, 2.0, 3.0, 9.0]),
            ])

        when:
            int minutes = service.rainDuration(120)
            double mm = service.rainAmount(120)

        then:
            // Two full raining hours inside the window (allow 1-min clock slack).
            minutes >= 118 && minutes <= 120
            // Only the now-1h (2.0mm) and now (3.0mm) slots contribute; ~5.0mm total.
            mm >= 4.9d && mm <= 5.0d
    }

    void "a future slot is excluded from both predicates"() {
        given:
            stubMeteoDevice([
                'timezone'            : 'GMT',
                'hourly.time'         : JsonOutput.toJson([slot(1), slot(2)]),
                'hourly.precipitation': JsonOutput.toJson([8.0, 8.0]),
            ])

        expect:
            service.rainDuration(120) == 0
            service.rainAmount(120) == 0.0d
    }

    void "precipitation at or below threshold adds no duration but still counts toward amount"() {
        given:
            // Threshold is 0.0, so a 0.0mm hour is not "raining" for duration,
            // while a measurable hour drives both. Slots end at now-1h and now.
            stubMeteoDevice([
                'timezone'            : 'GMT',
                'hourly.time'         : JsonOutput.toJson([slot(-1), slot(0)]),
                'hourly.precipitation': JsonOutput.toJson([0.0, 4.0]),
            ])

        when:
            int minutes = service.rainDuration(120)
            double mm = service.rainAmount(120)

        then:
            // Only the now slot (4.0 > 0) contributes duration: ~60 min.
            minutes >= 58 && minutes <= 60
            // Both slots count toward amount, but the 0.0mm hour adds nothing: ~4.0mm.
            mm >= 3.9d && mm <= 4.0d
    }
}
