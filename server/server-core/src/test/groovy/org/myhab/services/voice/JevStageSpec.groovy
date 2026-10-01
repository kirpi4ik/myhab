package org.myhab.services.voice

import spock.lang.Specification

/** Question building and answer mapping for the Jev stage (no network). */
class JevStageSpec extends Specification {

    static final Map CATALOG = [
        peripherals: [[id: 15L, name: 'Terrace Light', category: 'LIGHT', zones: ['Terrace'], aliases: ['porch light']]],
        zones      : [[id: 7L, name: 'Terrace', peripherals: ['Terrace Light', 'Terrace Lamp'], aliases: []]],
        scenarios  : [[jobId: 3L, name: 'Movie mode', description: 'Dim + TV']],
        mowers     : [[deviceId: 5L, name: 'Navimow']]
    ]

    void "questions cover every peripheral and zone, plus none"() {
        when:
            Map q = JevStage.questions(CATALOG)

        then:
            q.keySet() == ['intent', 'target', 'action', 'mower_action', 'scenario'] as Set
            q.target.criteria.keySet() as List == ['P15', 'Z7', 'none']
            q.target.criteria.P15 == 'DEVICE: Terrace Light (LIGHT) in Terrace; also called porch light'
            q.target.criteria.Z7 == 'AREA (everything in it): Terrace; contains Terrace Light, Terrace Lamp'
            q.scenario.criteria.keySet() as List == ['S3', 'none']
            q.action.criteria.OFF.contains('stinge')
    }

    void "no scenario question without scenarios"() {
        expect:
            !JevStage.questions(CATALOG + [scenarios: []]).containsKey('scenario')
    }

    void "a catalog beyond Jev's option limit is refused"() {
        given:
            Map big = CATALOG + [peripherals: (1..300).collect { [id: it, name: "P${it}", category: 'LIGHT', zones: [], aliases: []] }]

        when:
            JevStage.questions(big)

        then:
            thrown(IllegalStateException)
    }

    void "a control decision uses intent, target and action; confidence is their minimum"() {
        when:
            FastDecision d = JevStage.decide([
                intent      : [choice: 'control', confidence: 1.0],
                target      : [choice: 'P15', confidence: 0.8],
                action      : [choice: 'OFF', confidence: 0.95],
                mower_action: [choice: 'none', confidence: 0.1]
            ])

        then:
            d.stage == 'jev'
            d.key() == 'control|P15|OFF'
            d.confidence == 0.8d
            d.mowerAction == null
    }

    void "'none' answers become null and only the intent's questions count"() {
        when:
            FastDecision query = JevStage.decide([
                intent: [choice: 'query', confidence: 0.9], target: [choice: 'none', confidence: 0.7],
                action: [choice: 'ON', confidence: 0.2]
            ])
            FastDecision other = JevStage.decide([intent: [choice: 'other', confidence: 0.99]])

        then:
            query.target == null && query.action == null && query.confidence == 0.7d
            other.key() == 'other' && other.confidence == 0.99d
    }

    void "a missing answer counts as zero confidence"() {
        expect:
            JevStage.decide([intent: [choice: 'scenario', confidence: 0.99]]).confidence == 0d
    }

    void "resolve refuses to call Jev without an API key"() {
        when:
            new JevStage().resolve('turn on the light', 'en-US', [catalog: CATALOG])

        then:
            thrown(IllegalStateException)
    }
}
