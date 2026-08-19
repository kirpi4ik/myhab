package org.myhab.domain.ui

import grails.testing.gorm.DataTest
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironmentImpl
import spock.lang.Specification

/**
 * Drives the registered {@code dashboardScreenSaveLayout} fetcher rather than
 * {@code validateLayoutJson} directly: the fetcher body only resolves at runtime,
 * so a call that compiles fine can still fail there. It did — an unqualified
 * {@code validateLayoutJson(json)} inside the DSL closure resolved against the
 * closure instead of the enclosing class, breaking every layout save.
 */
class DashboardScreenGraphqlSpec extends Specification implements DataTest {

    private static final String VALID_LAYOUT = '{"version":1,"widgets":[' +
            '{"id":"w1","peripheralId":2524,"kind":"marker","x":87.5,"y":465.8,"size":44.9,"icon":null},' +
            '{"id":"w2","peripheralId":3648036,"kind":"zone","points":[[344.6,467.5],[422.8,619.5],[465.9,647.4]]},' +
            '{"id":"w3","kind":"link","x":57,"y":54.2,"size":64,"icon":"mdi-home","label":"","href":"/"}]}'

    void setupSpec() {
        mockDomains(DashboardScreen, DashboardScreenBackground)
    }

    private static DataFetcher saveLayoutFetcher() {
        def operation = DashboardScreen.graphql.initialize().customMutationOperations.find {
            it.name == 'dashboardScreenSaveLayout'
        }
        assert operation != null: 'dashboardScreenSaveLayout mutation is not registered'
        return operation.dataFetcher
    }

    private static Object saveLayout(Map arguments) {
        return saveLayoutFetcher().get(
                DataFetchingEnvironmentImpl.newDataFetchingEnvironment().arguments(arguments).build())
    }

    void "saveLayout persists a valid layout"() {
        given:
            DashboardScreen screen = new DashboardScreen(name: 'ground-floor').save(flush: true)

        when:
            DashboardScreen saved = saveLayout(id: screen.id, layoutJson: VALID_LAYOUT)

        then:
            saved.id == screen.id
            DashboardScreen.get(screen.id).layoutJson == VALID_LAYOUT
    }

    void "saveLayout rejects an unsupported version and leaves the screen untouched"() {
        given:
            DashboardScreen screen = new DashboardScreen(name: 'first-floor').save(flush: true)

        when:
            saveLayout(id: screen.id, layoutJson: '{"version":2,"widgets":[]}')

        then:
            RuntimeException e = thrown()
            e.message.startsWith('Invalid layout:')
            DashboardScreen.get(screen.id).layoutJson == null
    }

    void "saveLayout rejects a malformed widget"() {
        given:
            DashboardScreen screen = new DashboardScreen(name: 'attic').save(flush: true)

        when:
            saveLayout(id: screen.id, layoutJson: '{"version":1,"widgets":[{"id":"w1","kind":"marker"}]}')

        then:
            thrown(RuntimeException)
    }

    void "saveLayout fails for an unknown screen"() {
        when:
            saveLayout(id: 999999L, layoutJson: VALID_LAYOUT)

        then:
            RuntimeException e = thrown()
            e.message.contains('not found')
    }
}
