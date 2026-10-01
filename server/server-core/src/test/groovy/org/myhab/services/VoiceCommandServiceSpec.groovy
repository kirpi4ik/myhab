package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.services.voice.FastDecision
import org.myhab.services.voice.IntentStage
import org.myhab.services.voice.LlmTurn
import org.myhab.services.voice.NluSidecarStage
import org.myhab.services.voice.ToolCall
import org.myhab.services.voice.VoiceIntentProvider
import org.myhab.services.voice.VoiceTools
import spock.lang.Specification

/**
 * Unit specs for the v2 agentic loop. The LLM provider, the catalog, the
 * scheduler and Hazelcast are stubbed (no network, no GORM); {@code publish} is
 * intercepted so we can assert exactly what reaches UIMessageService.
 */
class VoiceCommandServiceSpec extends Specification implements ServiceUnitTest<VoiceCommandService>, DataTest {

    // handleTranscript is @Transactional; DataTest exists only to supply the
    // transaction manager. No domain class is touched here.
    Class[] getDomainClassesToMock() { [] as Class[] }

    List<Map> published
    VoiceIntentProvider provider
    IntentStage stage

    /** Config values by key; voice on, LLM = anthropic, TTS and the fast path off unless overridden. */
    private void configure(Map<String, Object> overrides = [:]) {
        Map<String, Object> values = [
            (CfgKey.VOICE.VOICE_ENABLED.key())     : true,
            (CfgKey.VOICE.VOICE_LLM_PROVIDER.key()): 'anthropic',
            (CfgKey.VOICE.VOICE_TTS_ENABLED.key()) : false
        ] + overrides
        service.configProvider = Stub(ConfigProvider) {
            get(_, _) >> { args -> values[args[1] as String] }
        }
    }

    private void activeNlu(Map<String, Object> extra = [:]) {
        configure([(CfgKey.VOICE.VOICE_NLU_ENABLED.key()): true,
                   (CfgKey.VOICE.VOICE_NLU_MODE.key())   : 'active'] + extra)
    }

    private static FastDecision decision(Map fields) {
        new FastDecision([stage: 'nlu', confidence: 0.97d] + fields)
    }

    def setup() {
        published = []
        provider = Mock(VoiceIntentProvider)
        provider.name() >> 'anthropic'
        provider.defaultModel() >> 'claude-haiku-4-5'

        configure()
        service.providers = ['anthropic': provider]
        stage = Mock(IntentStage)
        service.stages = ['nlu': stage]
        service.schedulerService = Mock(SchedulerService)
        service.navimowCommandService = Mock(org.myhab.services.dsl.action.NavimowCommandService)
        // Hazelcast: return a real map so load/save round-trips work.
        def map = [:]
        service.hazelcastInstance = Mock(HazelcastInstance) {
            getMap(_) >> Mock(com.hazelcast.map.IMap) {
                get(_) >> { args -> map.get(args[0]) }
                put(_, _, _, _) >> { args -> map.put(args[0], args[1]) }
            }
        }

        // Stub the catalog so the test does not need GORM.
        service.metaClass.buildCatalog = { ->
            [peripherals: [[id: 15L, name: 'Terrace Light', category: 'LIGHT', zones: ['Terrace'], aliases: []]],
             zones      : [[id: 7L, name: 'Terrace', peripherals: ['Terrace Light', 'Terrace Lamp'], aliases: []]],
             scenarios  : [[jobId: 3L, name: 'Movie mode', description: 'Dim + TV']],
             mowers     : [[deviceId: 5L, name: 'Navimow']]]
        }
        service.metaClass.publish = { String ns, Object data -> published << [ns: ns, data: data] }
    }

    private static LlmTurn toolTurn(String name, Map input) {
        new LlmTurn(toolCalls: [new ToolCall(id: 't1', name: name, input: input)])
    }

    private static LlmTurn textTurn(String text) {
        new LlmTurn(toolCalls: [], finalText: text)
    }

    void "a ZONE control turn switches the whole zone via evt_switch and completes"() {
        when:
            Map result = service.handleTranscript('turn off the terrace lights', 'en-US', null, 'tester')

        then: "loop: first a ZONE control tool call, then a final text"
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'ZONE', id: 7, action: 'OFF']),
                textTurn('Turned off the terrace.')
            ]

        and: "one evt_switch with p1=ZONE, p2=7, p4=off"
            published.size() == 1
            published[0].ns == 'evt_switch'
            published[0].data.p1 == 'ZONE'
            published[0].data.p2 == '7'
            published[0].data.p4 == 'off'

        and:
            result.success
            result.spokenResponse == 'Turned off the terrace.'
            !result.awaitingReply
            result.sessionId
    }

    void "an ambiguous turn asks back: no action, awaitingReply true"() {
        when:
            Map result = service.handleTranscript('turn off the light', 'en-US', null, 'tester')

        then: "model ends the turn with a clarifying question, no tool call"
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Which light — the terrace or the kitchen?')

        and:
            result.success
            result.awaitingReply
            result.spokenResponse.startsWith('Which light')
            published.isEmpty()
    }

    void "run_scenario triggers the job and does not publish a switch"() {
        when:
            Map result = service.handleTranscript('run movie mode', 'en-US', null, 'tester')

        then:
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.RUN_SCENARIO, [jobId: 3]),
                textTurn('Starting movie mode.')
            ]
            1 * service.schedulerService.triggerJob(3L, 'tester')
            published.isEmpty()
            result.success
            !result.awaitingReply
    }

    void "an out-of-catalog scenario id is rejected and triggers nothing"() {
        when:
            service.handleTranscript('run something', 'en-US', null, 'tester')

        then:
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.RUN_SCENARIO, [jobId: 999]),
                textTurn('I could not find that scenario.')
            ]
            0 * service.schedulerService.triggerJob(_, _)
            published.isEmpty()
    }

    void "an out-of-catalog control id is rejected and publishes nothing"() {
        when:
            service.handleTranscript('turn on the ghost', 'en-US', null, 'tester')

        then:
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 999, action: 'ON']),
                textTurn('I could not find that device.')
            ]
            published.isEmpty()
    }

    void "a tool failure is fed back to the model and does not abort the turn"() {
        when:
            Map result = service.handleTranscript('run movie mode', 'en-US', null, 'tester')

        then: "triggerJob throws, but the loop continues and the model explains"
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.RUN_SCENARIO, [jobId: 3]),
                textTurn('Sorry, I could not start movie mode.')
            ]
            1 * service.schedulerService.triggerJob(3L, 'tester') >> { throw new IllegalStateException('Quartz down') }

        and:
            result.success
            result.spokenResponse == 'Sorry, I could not start movie mode.'
            published.isEmpty()
    }

    void "mower_command sends the action to the Navimow service"() {
        when:
            Map result = service.handleTranscript('start mowing', 'en-US', null, 'tester')

        then:
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.MOWER_COMMAND, [deviceId: 5, action: 'START']),
                textTurn('Starting the mower.')
            ]
            1 * service.navimowCommandService.execute([deviceId: 5L, action: 'START'])
            published.isEmpty()
            result.success
            !result.awaitingReply
    }

    void "an out-of-catalog mower id is rejected and sends nothing"() {
        when:
            service.handleTranscript('start the other mower', 'en-US', null, 'tester')

        then:
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.MOWER_COMMAND, [deviceId: 999, action: 'START']),
                textTurn('I could not find that mower.')
            ]
            0 * service.navimowCommandService.execute(_)
            published.isEmpty()
    }

    void "the feature toggle gates execution"() {
        given:
            service.configProvider = Mock(ConfigProvider) {
                get(Boolean, CfgKey.VOICE.VOICE_ENABLED.key()) >> false
            }

        when:
            Map result = service.handleTranscript('turn off the terrace lights', 'en-US', null, 'tester')

        then:
            !result.success
            0 * provider.converse(_, _, _, _, _, _)
            published.isEmpty()
    }

    // ------------------------------------------------------------- fast path

    void "the fast path is not consulted while disabled"() {
        when:
            service.handleTranscript('turn off the terrace lights', 'en-US', null, 'tester')

        then:
            0 * stage.resolve(_, _, _)
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
    }

    void "an active decision above the gate is executed without the LLM, with a reply in the locale's language"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('turn on the terrace light', locale, null, 'tester')

        then:
            1 * stage.resolve('turn on the terrace light', locale, _) >> decision(intent: 'control', target: 'P15', action: 'ON')
            0 * provider.converse(_, _, _, _, _, _)

        and: "the same evt_switch the LLM's control_entity would publish"
            published.size() == 1
            published[0].data.p1 == 'PERIPHERAL'
            published[0].data.p2 == '15'
            published[0].data.p4 == 'on'

        and:
            result.success
            result.resolvedBy == 'nlu'
            !result.awaitingReply
            result.spokenResponse == reply

        where:
            locale  | reply
            'en-US' | 'Turned on Terrace Light.'
            'ro-RO' | 'Am pornit Terrace Light.'
    }

    void "a decision below the gate falls through to the LLM"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON', confidence: 0.45d)
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 15, action: 'ON']),
                textTurn('Turned on the terrace light.')
            ]
            published.size() == 1
            result.resolvedBy == 'llm'
    }

    void "the gate is configurable"() {
        given:
            activeNlu([(CfgKey.VOICE.VOICE_NLU_GATE.key()): 0.9d])

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then: "0.8 clears the default gate but not a configured 0.9"
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON', confidence: 0.8d)
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
            result.resolvedBy == 'llm'
    }

    void "intents the fast path does not execute fall through: #intent"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('something', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: intent, target: 'P15', confidence: 0.99d)
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Which one?')
            published.isEmpty()
            result.resolvedBy == 'llm'

        where:
            intent << ['query', 'clarify', 'other']
    }

    void "a zone-wide OFF needs the stricter confidence"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('turn off everything on the terrace', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'Z7', action: 'OFF', confidence: confidence)
            (llmCalls) * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
            result.resolvedBy == resolvedBy
            published.size() == (resolvedBy == 'nlu' ? 1 : 0)

        where:
            confidence | llmCalls | resolvedBy
            0.92d      | 1        | 'llm'
            0.97d      | 0        | 'nlu'
    }

    void "a failing stage falls through to the LLM"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> { throw new IllegalStateException('sidecar down') }
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
            result.success
            result.resolvedBy == 'llm'
    }

    void "a decision for an id outside the catalog is not executed and falls through"() {
        given:
            activeNlu()

        when:
            Map result = service.handleTranscript('turn on the ghost', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P999', action: 'ON')
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('I could not find that device.')
            published.isEmpty()
            result.resolvedBy == 'llm'
    }

    void "scenario and mower decisions run through the same tools"() {
        given:
            activeNlu()

        when:
            Map scenario = service.handleTranscript('run movie mode', 'en-US', null, 'tester')
            Map mower = service.handleTranscript('send the mower home', 'ro-RO', null, 'tester')

        then:
            2 * stage.resolve(_, _, _) >>> [
                decision(intent: 'scenario', scenario: 'S3'),
                decision(intent: 'mower', mowerAction: 'DOCK')
            ]
            1 * service.schedulerService.triggerJob(3L, 'tester')
            1 * service.navimowCommandService.execute([deviceId: 5L, action: 'DOCK'])
            0 * provider.converse(_, _, _, _, _, _)
            scenario.spokenResponse == 'Started Movie mode.'
            mower.spokenResponse == 'Robotul de tuns se întoarce la bază.'
    }

    void "shadow mode resolves but the LLM still acts"() {
        given:
            configure([(CfgKey.VOICE.VOICE_NLU_ENABLED.key()): true])  // mode defaults to shadow

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON', confidence: 0.99d)
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 15, action: 'ON']),
                textTurn('Turned on the terrace light.')
            ]
            published.size() == 1
            result.resolvedBy == 'llm'
    }

    void "an answer to the assistant's question stays with the LLM"() {
        given:
            activeNlu()

        when: "the LLM asks back"
            Map first = service.handleTranscript('turn on the light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'clarify')
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Which light, the terrace or the kitchen?')
            first.awaitingReply

        when: "the user answers in the same session"
            service.handleTranscript('the terrace', 'en-US', first.sessionId, 'tester')

        then: "the fast path is skipped"
            0 * stage.resolve(_, _, _)
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
    }

    // ------------------------------------------------------------- Jev stage

    void "when NLU is unsure, an active Jev decision above its gate is executed"() {
        given:
            IntentStage jev = Mock(IntentStage)
            service.stages = ['nlu': stage, 'jev': jev]
            activeNlu([(CfgKey.VOICE.VOICE_JEV_ENABLED.key()): true, (CfgKey.VOICE.VOICE_JEV_MODE.key()): 'active'])

        when:
            Map result = service.handleTranscript('stinge lumina de pe terasa', 'ro-RO', null, 'tester')

        then: "NLU first, below its 0.6 gate"
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'OFF', confidence: 0.4d)

        then: "Jev gets the catalog and clears its 0.7 gate"
            1 * jev.resolve('stinge lumina de pe terasa', 'ro-RO', { it.catalog?.peripherals }) >>
                    new FastDecision(stage: 'jev', intent: 'control', target: 'P15', action: 'OFF', confidence: 0.8d)
            0 * provider.converse(_, _, _, _, _, _)
            published.size() == 1
            published[0].data.p4 == 'off'
            result.resolvedBy == 'jev'
            result.spokenResponse == 'Am oprit Terrace Light.'
    }

    void "Jev is not asked when NLU already acted, nor when it is disabled"() {
        given:
            IntentStage jev = Mock(IntentStage)
            service.stages = ['nlu': stage, 'jev': jev]
            activeNlu(jevEnabled ? [(CfgKey.VOICE.VOICE_JEV_ENABLED.key()): true, (CfgKey.VOICE.VOICE_JEV_MODE.key()): 'active'] : [:])

        when:
            service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON', confidence: nluConfidence)
            0 * jev.resolve(_, _, _)
            (llmCalls) * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')

        where:
            jevEnabled | nluConfidence | llmCalls
            true       | 0.97d         | 0
            false      | 0.3d          | 1
    }

    void "when both stages are unsure, the LLM decides"() {
        given:
            IntentStage jev = Mock(IntentStage)
            service.stages = ['nlu': stage, 'jev': jev]
            activeNlu([(CfgKey.VOICE.VOICE_JEV_ENABLED.key()): true, (CfgKey.VOICE.VOICE_JEV_MODE.key()): 'active'])

        when:
            Map result = service.handleTranscript('turn on the light in the bedroom', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON', confidence: 0.3d)
            1 * jev.resolve(_, _, _) >> new FastDecision(stage: 'jev', intent: 'control', target: 'Z7', action: 'ON', confidence: 0.45d)
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Which bedroom?')
            published.isEmpty()
            result.resolvedBy == 'llm'
            result.awaitingReply
    }

    void "a failing Jev call falls through to the LLM"() {
        given:
            IntentStage jev = Mock(IntentStage)
            service.stages = ['jev': jev]
            configure([(CfgKey.VOICE.VOICE_JEV_ENABLED.key()): true, (CfgKey.VOICE.VOICE_JEV_MODE.key()): 'active'])

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * jev.resolve(_, _, _) >> { throw new IllegalStateException('Jev HTTP 529') }
            1 * provider.converse(_, _, _, _, _, _) >> textTurn('Done.')
            result.resolvedBy == 'llm'
    }

    void "metrics: stage timings with histogram buckets, resolutions per stage, shadow agreement"() {
        given:
            def registry = new io.micrometer.prometheus.PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT)
            service.meterRegistry = registry
            configure([(CfgKey.VOICE.VOICE_NLU_ENABLED.key()): true])  // shadow

        when:
            service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * stage.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON')
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 15, action: 'ON']),
                textTurn('Turned on the terrace light.')
            ]
            registry.get('voice.stage.duration').tags('stage', 'nlu', 'outcome', 'shadow-accept').timer().count() == 1
            registry.get('voice.resolved').tags('stage', 'llm').counter().count() == 1
            registry.get('voice.shadow').tags('stage', 'nlu', 'accepted', 'true', 'agree', 'true').counter().count() == 1

        and: "the series the Grafana dashboard queries"
            String scrape = registry.scrape()
            scrape.contains('voice_stage_duration_seconds_bucket{outcome="accepted",stage="llm",le=')
            scrape.contains('voice_resolved_total{stage="llm"')
            scrape.contains('voice_shadow_total{accepted="true",agree="true",stage="nlu"')
    }

    // ------------------------------------------------------------- training feedback

    private NluSidecarStage feedbackNlu(Map<String, Object> extra = [:]) {
        NluSidecarStage nlu = Mock(NluSidecarStage)
        service.stages = ['nlu': nlu]
        service.feedbackExecutor = { Runnable r -> r.run() } as java.util.concurrent.Executor
        activeNlu(extra)
        return nlu
    }

    void "the LLM's single successful tool call is sent as a training label"() {
        given:
            NluSidecarStage nlu = feedbackNlu()

        when:
            service.handleTranscript('make the porch bright', 'en-US', null, 'tester')

        then:
            1 * nlu.resolve(_, _, _) >> decision(intent: 'control', target: 'Z7', action: 'ON', confidence: 0.3d, catalogHash: 'h1')
            2 * provider.converse(_, _, _, _, _, _) >>> [
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 15, action: 'on']),
                textTurn('Turned on the terrace light.')
            ]
            1 * nlu.postFeedback(_, { Map r ->
                r.text == 'make the porch bright' && r.resolvedBy == 'llm' && r.labelSource == 'llm' &&
                r.label == [intent: 'control', target: 'P15', action: 'ON'] &&
                r.stages == [[stage: 'nlu', key: 'control|Z7|ON', confidence: 0.3d, accepted: false, mode: 'active', catalogHash: 'h1']]
            })
    }

    void "no label when the LLM did not settle it with exactly one working call: #why"() {
        given:
            NluSidecarStage nlu = feedbackNlu()
            service.schedulerService.triggerJob(_, _) >> { throw new IllegalStateException('Quartz down') }

        when:
            service.handleTranscript('something', 'en-US', null, 'tester')

        then:
            1 * nlu.resolve(_, _, _) >> null
            _ * provider.converse(_, _, _, _, _, _) >>> turns
            1 * nlu.postFeedback(_, { it.label == null && it.resolvedBy == 'llm' })

        where:
            why                  | turns
            'two calls'          | [new LlmTurn(toolCalls: [new ToolCall(id: 'a', name: VoiceTools.CONTROL_ENTITY, input: [entityType: 'PERIPHERAL', id: 15, action: 'ON']),
                                                          new ToolCall(id: 'b', name: VoiceTools.RUN_SCENARIO, input: [jobId: 3])]), textTurn('Done.')]
            'the call failed'    | [toolTurn(VoiceTools.RUN_SCENARIO, [jobId: 3]), textTurn('Sorry.')]
            'id not in catalog'  | [toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 999, action: 'ON']), textTurn('Not found.')]
            'asked back'         | [textTurn('Which one?')]
    }

    void "a fast-path decision is logged, never as a label"() {
        given:
            NluSidecarStage nlu = feedbackNlu()

        when:
            service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * nlu.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON')
            0 * provider.converse(_, _, _, _, _, _)
            1 * nlu.postFeedback(_, { it.resolvedBy == 'nlu' && it.label == null && it.stages[0].accepted })
    }

    void "an answer to the assistant's question is never a label"() {
        given:
            NluSidecarStage nlu = feedbackNlu()

        when:
            Map first = service.handleTranscript('turn on the light', 'en-US', null, 'tester')
            service.handleTranscript('the terrace one', 'en-US', first.sessionId, 'tester')

        then:
            1 * nlu.resolve(_, _, _) >> decision(intent: 'clarify', confidence: 0.9d)
            3 * provider.converse(_, _, _, _, _, _) >>> [
                textTurn('Which light?'),
                toolTurn(VoiceTools.CONTROL_ENTITY, [entityType: 'PERIPHERAL', id: 15, action: 'ON']),
                textTurn('Turned on the terrace light.')
            ]
            2 * nlu.postFeedback(_, { it.label == null })
    }

    void "no feedback when it is turned off"() {
        given:
            NluSidecarStage nlu = feedbackNlu([(CfgKey.VOICE.VOICE_NLU_FEEDBACK.key()): false])

        when:
            service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')

        then:
            1 * nlu.resolve(_, _, _) >> decision(intent: 'control', target: 'P15', action: 'ON')
            0 * nlu.postFeedback(_, _)
    }

    void "a fast-path turn is kept in the history as plain text for the next LLM turn"() {
        given:
            activeNlu()

        when:
            Map first = service.handleTranscript('turn on the terrace light', 'en-US', null, 'tester')
            service.handleTranscript('and what is the weather', 'en-US', first.sessionId, 'tester')

        then:
            2 * stage.resolve(_, _, _) >>> [
                decision(intent: 'control', target: 'P15', action: 'ON'),
                decision(intent: 'other', confidence: 0.99d)
            ]
            1 * provider.converse(_, _, { List<Map> m ->
                m*.role == ['user', 'assistant', 'user'] && m[1].text == 'Turned on Terrace Light.' && !m[1].toolCalls
            }, _, _, _) >> textTurn('I can only help with the house.')
    }
}
