package org.myhab.services.dsl

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

/**
 * Specs for {@link DslService#execute}.
 *
 * <p>Covers the {@code DELEGATE_FIRST} resolution the scenario DSL relies on:
 * bare names resolve through {@link CompositeDelegate} — knowledge service first,
 * scenario service last, its {@code methodMissing} as the final fallback.</p>
 *
 * <p>Uses plain stubs rather than Spock mocks: {@code CompositeDelegate}
 * dispatches via {@code metaClass.respondsTo}, which does not report generated
 * mock methods reliably.</p>
 */
class DslServiceSpec extends Specification implements ServiceUnitTest<DslService>, DataTest {

    // DslService is @Transactional; DataTest exists only to supply the
    // transaction manager. No domain class is touched here.
    Class[] getDomainClassesToMock() { [] as Class[] }

    StubKnowledge knowledge
    StubScenario scenario

    def setup() {
        knowledge = new StubKnowledge()
        scenario = new StubScenario()
        service.knowledgeService = knowledge
        service.scenarioService = scenario
    }

    void "resolves a predicate from the knowledge delegate"() {
        given:
            knowledge.rainy = raining

        expect:
            service.execute('isRaining()') == raining

        where:
            raining << [true, false]
    }

    void "resolves an action from the scenario delegate and returns its result"() {
        when:
            def result = service.execute('switchOn([portIds: [123]])')

        then:
            result == 'switched'
            scenario.calls == [[op: 'switchOn', args: [portIds: [123]]]]
    }

    void "falls through to the scenario service methodMissing for unknown names"() {
        when:
            def result = service.execute('sprinkle([zone: 4])')

        then: "ScenarioService is last, so its methodMissing resolves DslCommand beans"
            result == 'fallback'
            scenario.calls[0].op == 'sprinkle'
    }

    void "binds the audit actor for the duration of the run and clears it afterwards"() {
        when:
            service.execute('switchOn([portIds: [1]])', 'CRON')

        then:
            scenario.actorDuringCall == ['CRON']
            scenario.actor == null
    }

    void "defaults the actor to SYSTEM"() {
        when:
            service.execute('switchOn([portIds: [1]])')

        then:
            scenario.actorDuringCall == ['SYSTEM']
    }

    void "rejects a blank scenario"() {
        when:
            service.execute(script)

        then:
            thrown(IllegalArgumentException)

        where:
            script << [null, '', '   ']
    }

    void "wraps evaluation failures and still clears the actor"() {
        when:
            service.execute('throw new IllegalStateException("boom")', 'CRON')

        then:
            def ex = thrown(RuntimeException)
            ex.message.contains('Scenario execution failed')
            scenario.actor == null
    }
}

// Top-level, not static inner: Groovy forbids methodMissing on static inner
// classes (it reserves a synthetic one for outer-class delegation).
class StubKnowledge {
    boolean rainy = false

    boolean isRaining() { rainy }
}

class StubScenario {
    List<Map> calls = []
    String actor
    List<String> actorDuringCall = []

    void setExecutionActor(String a) { actor = a }

    void clearExecutionActor() { actor = null }

    def switchOn(args) {
        actorDuringCall << actor
        calls << [op: 'switchOn', args: args]
        return 'switched'
    }

    def methodMissing(String name, args) {
        calls << [op: name, args: args as List]
        return 'fallback'
    }
}
