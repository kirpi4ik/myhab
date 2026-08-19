package org.myhab.domain

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class TimeSeriesStatisticSpec extends Specification implements DomainUnitTest<TimeSeriesStatistic> {

    void "deltaDiff is optional"() {
        given:
            domain.key = 'emeters.dev-1.hourly'
            domain.value = 12.5d
            domain.deltaDiff = null

        expect:
            domain.validate()
    }

    void "key and value are required"() {
        when:
            def stat = new TimeSeriesStatistic(key: key, value: value)

        then:
            !stat.validate()
            stat.errors[rejected]?.code == 'nullable'

        where:
            key                    | value || rejected
            null                   | 12.5d || 'key'
            'emeters.dev-1.hourly' | null  || 'value'
    }

    void "a valid statistic persists and beforeInsert stamps tsCreated"() {
        when:
            def stat = new TimeSeriesStatistic(key: 'emeters.dev-1.hourly', value: 3.5d, deltaDiff: 0.25d)
                    .save(flush: true, failOnError: true)

        then:
            stat.id != null
            TimeSeriesStatistic.count() == 1
            stat.tsCreated != DomainUtil.NULL_DATE
            stat.tsUpdated == stat.tsCreated
    }
}
