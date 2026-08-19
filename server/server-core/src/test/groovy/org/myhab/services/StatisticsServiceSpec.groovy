package org.myhab.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.domain.TimeSeriesStatistic
import org.myhab.domain.device.Device
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortValue
import org.myhab.domain.events.TopicName
import spock.lang.Specification

/**
 * Specs for {@link StatisticsService#saveActivePowerStatistics}.
 *
 * <p>Uses real in-memory GORM rather than stubbed finders so the service's
 * {@code createCriteria} queries are genuinely exercised.</p>
 */
class StatisticsServiceSpec extends Specification implements ServiceUnitTest<StatisticsService>, DataTest {

    Class[] getDomainClassesToMock() { [Device, DevicePort, PortValue, TimeSeriesStatistic] as Class[] }

    private static final String DEVICE_CODE = 'emeter-01'

    /** Date.parse needs groovy-dateutil, which is not on the test classpath. */
    private static Date at(String minute) {
        new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm').parse(minute)
    }

    private static final Date INTERVAL_START = at('2026-01-01 10:00')
    private static final Date INTERVAL_END = at('2026-01-01 11:00')
    private static final Date INSIDE_INTERVAL = at('2026-01-01 10:30')

    List<Map> published

    def setup() {
        published = []
        service.metaClass.publish = { String ns, Object data -> published << [ns: ns, data: data] }
    }

    /** Seeds the 'total_active_power' port the service looks up, plus a reading. */
    private DevicePort seed(String portReading, String readingAt = null) {
        Device device = new Device(code: DEVICE_CODE).save(flush: true, failOnError: true)
        DevicePort port = new DevicePort(internalRef: 'total_active_power', value: '25.500', device: device)
                .save(flush: true, failOnError: true)
        if (portReading != null) {
            new PortValue(portId: port.id, value: portReading,
                    tsCreated: readingAt ? at(readingAt) : INSIDE_INTERVAL)
                    .save(flush: true, failOnError: true)
        }
        return port
    }

    void "aggregates active power against the interval reading and publishes the change"() {
        given:
            seed('10.250')

        when:
            service.saveActivePowerStatistics('hourly', INTERVAL_START, INTERVAL_END)

        then: "aggregated value is the port total minus the reading in the interval"
            TimeSeriesStatistic.count() == 1
            def stat = TimeSeriesStatistic.first()
            stat.key == "emeters.${DEVICE_CODE}.hourly"
            stat.value.round(3) == 15.25d
            stat.deltaDiff == null

        and: "the change is announced on the stat topic"
            published.size() == 1
            published[0].ns == TopicName.EVT_STAT_VALUE_CHANGED.id()
            published[0].data.p2 == "emeters.${DEVICE_CODE}.hourly"
            // Event p0..p6 are Strings, so the id arrives coerced.
            published[0].data.p3 == stat.id as String
    }

    void "records deltaDiff against an existing statistic for the same key"() {
        given: "exactly one prior statistic, so ordering cannot affect the outcome"
            seed('10.250')
            new TimeSeriesStatistic(key: "emeters.${DEVICE_CODE}.hourly", value: 15.00d)
                    .save(flush: true, failOnError: true)

        when:
            service.saveActivePowerStatistics('hourly', INTERVAL_START, INTERVAL_END)

        then:
            TimeSeriesStatistic.count() == 2
            def latest = TimeSeriesStatistic.list().find { it.deltaDiff != null }
            latest.value.round(3) == 15.25d
            latest.deltaDiff.round(2) == 0.25d
    }

    void "does nothing when no port reading falls inside the interval"() {
        given: "the only reading is an hour before the interval opens"
            seed('10.250', '2026-01-01 09:00')

        when:
            service.saveActivePowerStatistics('hourly', INTERVAL_START, INTERVAL_END)

        then:
            TimeSeriesStatistic.count() == 0
            published.isEmpty()
    }
}
