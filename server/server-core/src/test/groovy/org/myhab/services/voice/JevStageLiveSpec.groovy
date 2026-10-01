package org.myhab.services.voice

import spock.lang.Requires
import spock.lang.Specification

/** Calls the real Jev API; runs only when JEV_API_KEY is set (skipped in CI). */
@Requires({ System.getenv('JEV_API_KEY') })
class JevStageLiveSpec extends Specification {

    void "Jev resolves a Romanian command against a small catalog"() {
        when:
            FastDecision d = new JevStage().resolve('stinge lumina de pe terasă', 'ro-RO',
                    [apiKey: System.getenv('JEV_API_KEY'), timeoutMs: 10000, catalog: JevStageSpec.CATALOG])

        then:
            d.intent == 'control'
            d.target in ['P15', 'Z7']
            d.action == 'OFF'
            d.confidence > 0
    }
}
