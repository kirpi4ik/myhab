package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import grails.testing.services.ServiceUnitTest
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.services.voice.LlmTurn
import org.myhab.services.voice.ToolCall
import org.myhab.services.voice.VoiceIntentProvider
import org.myhab.services.voice.VoiceTools
import spock.lang.Specification

/**
 * Unit specs for the v2 agentic loop. The LLM provider, the catalog, the
 * scheduler and Hazelcast are stubbed (no network, no GORM); {@code publish} is
 * intercepted so we can assert exactly what reaches UIMessageService.
 */
class VoiceCommandServiceSpec extends Specification implements ServiceUnitTest<VoiceCommandService> {

    List<Map> published
    VoiceIntentProvider provider

    def setup() {
        published = []
        provider = Mock(VoiceIntentProvider)
        provider.name() >> 'anthropic'
        provider.defaultModel() >> 'claude-haiku-4-5'

        service.configProvider = Mock(ConfigProvider) {
            get(Boolean, CfgKey.VOICE.VOICE_ENABLED.key()) >> true
            get(String, CfgKey.VOICE.VOICE_LLM_PROVIDER.key()) >> 'anthropic'
            // TTS disabled by default in tests
            get(Boolean, CfgKey.VOICE.VOICE_TTS_ENABLED.key()) >> false
        }
        service.providers = ['anthropic': provider]
        service.schedulerService = Mock(SchedulerService)
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
             scenarios  : [[jobId: 3L, name: 'Movie mode', description: 'Dim + TV']]]
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
            1 * service.schedulerService.triggerJob(3L)
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
            0 * service.schedulerService.triggerJob(_)
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
            1 * service.schedulerService.triggerJob(3L) >> { throw new IllegalStateException('Quartz down') }

        and:
            result.success
            result.spokenResponse == 'Sorry, I could not start movie mode.'
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
}
