package org.myhab.domain.device.port

import grails.testing.gorm.DataTest
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironmentImpl
import org.myhab.domain.job.EventData
import spock.lang.Specification

/**
 * {@code recentPortValues} declares a local named after the domain property it
 * filters on, so its where-clause reads {@code portId == portId}. That is not a
 * self-comparison: GORM's where-closure transform binds the left side to the
 * property and the right side to the in-scope local. These tests pin that down —
 * the query really does filter, and stays that way across GORM upgrades.
 */
class PortValueGraphqlSpec extends Specification implements DataTest {

    void setupSpec() {
        mockDomains(PortValue, EventData)
    }

    private static DataFetcher recentPortValues() {
        def operation = PortValue.graphql.initialize().customQueryOperations.find {
            it.name == 'recentPortValues'
        }
        assert operation != null: 'recentPortValues query is not registered'
        return operation.dataFetcher
    }

    private static List fetch(Map arguments) {
        return recentPortValues().get(
                DataFetchingEnvironmentImpl.newDataFetchingEnvironment().arguments(arguments).build())
    }

    void "recentPortValues returns only the requested port"() {
        given:
            new PortValue(portId: 1L, value: 'ON').save(flush: true)
            new PortValue(portId: 1L, value: 'OFF').save(flush: true)
            new PortValue(portId: 2L, value: 'ON').save(flush: true)

        when:
            List<PortValue> values = fetch(portId: 1L, limit: 10)

        then:
            values.size() == 2
            values.every { it.portId == 1L }
    }

    void "recentPortValues honours the limit"() {
        given:
            (1..5).each { new PortValue(portId: 3L, value: "v$it").save(flush: true) }

        expect:
            fetch(portId: 3L, limit: 2).size() == 2
    }

    void "recentPortValues returns nothing for a port with no readings"() {
        given:
            new PortValue(portId: 4L, value: 'ON').save(flush: true)

        expect:
            fetch(portId: 999L, limit: 10).isEmpty()
    }
}
