package org.myhab.services

import grails.testing.services.ServiceUnitTest
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.services.voice.VoiceIntentProvider
import spock.lang.Specification

/**
 * Unit specs for the voice pipeline orchestration. The LLM provider and the
 * peripheral catalog are stubbed (no network, no GORM); {@code publish} is
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
        }
        service.providers = ['anthropic': provider]

        // Stub the catalog so the test does not need a GORM context.
        service.metaClass.controllablePeripheralCatalog = { ->
            [[id: 42L, name: 'Terrace Light', category: 'LIGHT', zones: ['Terrace']]]
        }
        // Capture events instead of pushing them onto the real bus.
        service.metaClass.publish = { String ns, Object data -> published << [ns: ns, data: data] }
    }

    void "a resolved command publishes an evt_switch event for the matched peripheral"() {
        given:
            provider.resolveIntent(_, _, _, _) >> [peripheralId: 42, action: 'ON',
                                                   confidence: 0.95, spokenResponse: 'Terrace light on']

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', 'tester')

        then:
            result.success
            result.peripheralId == 42L
            result.action == 'ON'
            result.peripheralName == 'Terrace Light'

        and: "exactly one evt_switch event carrying PERIPHERAL/42/on"
            published.size() == 1
            published[0].ns == 'evt_switch'
            published[0].data.p1 == 'PERIPHERAL'
            published[0].data.p2 == '42'
            published[0].data.p4 == 'on'
    }

    void "an unresolved command returns failure and executes nothing"() {
        given:
            provider.resolveIntent(_, _, _, _) >> [peripheralId: null, action: 'ON', error: 'No matching peripheral']

        when:
            Map result = service.handleTranscript('do something vague', 'en-US', 'tester')

        then:
            !result.success
            result.error == 'No matching peripheral'
            published.isEmpty()
    }

    void "an out-of-catalog id is rejected and executes nothing"() {
        given:
            provider.resolveIntent(_, _, _, _) >> [peripheralId: 999, action: 'ON']

        when:
            Map result = service.handleTranscript('turn on the ghost light', 'en-US', 'tester')

        then:
            !result.success
            published.isEmpty()
    }

    void "an unknown configured provider fails cleanly"() {
        given:
            service.configProvider = Mock(ConfigProvider) {
                get(Boolean, CfgKey.VOICE.VOICE_ENABLED.key()) >> true
                get(String, CfgKey.VOICE.VOICE_LLM_PROVIDER.key()) >> 'does-not-exist'
            }

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', 'tester')

        then:
            !result.success
            published.isEmpty()
    }

    void "the feature toggle gates execution"() {
        given:
            service.configProvider = Mock(ConfigProvider) {
                get(Boolean, CfgKey.VOICE.VOICE_ENABLED.key()) >> false
            }

        when:
            Map result = service.handleTranscript('turn on the terrace light', 'en-US', 'tester')

        then:
            !result.success
            0 * provider.resolveIntent(_, _, _, _)
            published.isEmpty()
    }
}
