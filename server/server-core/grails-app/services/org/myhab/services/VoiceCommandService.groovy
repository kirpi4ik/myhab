package org.myhab.services

import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.EntityType
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.job.EventData
import org.myhab.services.voice.AnthropicIntentProvider
import org.myhab.services.voice.OpenAiIntentProvider
import org.myhab.services.voice.VoiceIntentProvider

/**
 * Voice-assistant pipeline: turn a transcribed English phrase into an on/off/
 * toggle command for a single peripheral.
 *
 * <p>The pipeline is client-agnostic and vendor-agnostic — the same flow serves
 * the PWA push-to-talk page and any future native client, and the LLM vendor is
 * selected by config behind {@link VoiceIntentProvider}. Steps:</p>
 * <ol>
 *   <li>build the catalog of controllable peripherals (grounding context),</li>
 *   <li>ask the configured provider to pick one id + action,</li>
 *   <li>validate the returned id is actually in the catalog (hallucination
 *       guard) and the action is supported,</li>
 *   <li>publish an {@code evt_switch} event so {@link UIMessageService} resolves
 *       ports, calls {@code PowerService} and writes the audit log — the same
 *       path the Web UI uses.</li>
 * </ol>
 */
@Slf4j
class VoiceCommandService implements EventPublisher {

    ConfigProvider configProvider

    /** LLM providers by name; selected via feature.voice.llm.provider. Replaceable in tests. */
    Map<String, VoiceIntentProvider> providers = [
        (new AnthropicIntentProvider().name()): new AnthropicIntentProvider(),
        (new OpenAiIntentProvider().name())   : new OpenAiIntentProvider()
    ]

    /** Topic handled by UIMessageService.receiveSwitchEvent (generic on/off). */
    static final String EVT_SWITCH = 'evt_switch'
    static final String DEFAULT_PROVIDER = 'anthropic'
    static final List<String> ACTIONS = ['ON', 'OFF', 'TOGGLE']

    /**
     * Resolve and execute a transcript. Always returns a result map shaped like
     * the {@code VoiceCommandResult} GraphQL type; never throws.
     */
    @Transactional
    Map handleTranscript(String transcript, String locale, String username = 'voice') {
        if (!isEnabled()) {
            return fail('Voice feature is disabled', transcript)
        }
        if (!transcript?.trim()) {
            return fail('Empty transcript', transcript)
        }

        VoiceIntentProvider provider = currentProvider()
        if (!provider) {
            return fail("Unknown LLM provider '${providerName()}'", transcript)
        }

        List<Map> catalog = controllablePeripheralCatalog()
        if (catalog.isEmpty()) {
            return fail('No controllable peripherals are configured', transcript)
        }

        Map intent
        try {
            intent = provider.resolveIntent(transcript, catalog, model(provider), apiKey(provider.name()))
        } catch (Exception e) {
            log.error("Voice intent resolution failed (provider=${provider.name()}) for transcript='${transcript}'", e)
            return fail("Could not understand the command: ${e.message}", transcript)
        }

        if (!intent || intent.error || intent.peripheralId == null) {
            return fail(intent?.error ?: 'No matching peripheral found', transcript, intent?.spokenResponse)
        }

        Long peripheralId = intent.peripheralId as Long
        String action = (intent.action as String)?.toUpperCase()

        Map match = catalog.find { it.id == peripheralId }
        if (!match) {
            log.warn("Voice: ${provider.name()} returned out-of-catalog peripheralId=${peripheralId}")
            return fail("Resolved peripheral is not controllable", transcript)
        }
        if (!(action in ACTIONS)) {
            return fail("Unsupported action: ${action}", transcript)
        }

        publishSwitch(peripheralId, action, transcript, username)
        log.info("Voice command: '${transcript}' -> peripheral ${peripheralId} (${match.name}) ${action}")

        return [
            success       : true,
            error         : null,
            transcript    : transcript,
            action        : action,
            peripheralId  : peripheralId,
            peripheralName: match.name,
            spokenResponse: intent.spokenResponse ?: "${match.name} turned ${action.toLowerCase()}".toString()
        ]
    }

    /**
     * Peripherals with at least one connected port, as the LLM grounding catalog.
     * Kept small ({@code id, name, category, zones}) on purpose — see plan.
     */
    List<Map> controllablePeripheralCatalog() {
        DevicePeripheral.list().findAll { it.connectedTo }.collect { p ->
            [
                id      : p.id,
                name    : p.name,
                category: p.category?.name,
                zones   : (p.zones*.name ?: []) as List
            ]
        }
    }

    /** Publish the generic switch event consumed by UIMessageService. */
    private void publishSwitch(Long peripheralId, String action, String transcript, String username) {
        publish(EVT_SWITCH, new EventData().with {
            p0 = EVT_SWITCH
            p1 = EntityType.PERIPHERAL.name()
            p2 = peripheralId.toString()
            p3 = "Voice assistant: ${username}"
            p4 = action.toLowerCase()
            p5 = groovy.json.JsonOutput.toJson([transcript: transcript])
            p6 = username
            it
        })
    }

    boolean isEnabled() {
        cfg(Boolean, CfgKey.VOICE.VOICE_ENABLED.key(), false)
    }

    private String providerName() {
        cfg(String, CfgKey.VOICE.VOICE_LLM_PROVIDER.key(), DEFAULT_PROVIDER)?.toLowerCase()
    }

    private VoiceIntentProvider currentProvider() {
        providers[providerName()]
    }

    private String model(VoiceIntentProvider provider) {
        cfg(String, CfgKey.VOICE.VOICE_LLM_MODEL.key(), provider.defaultModel())
    }

    /**
     * API key: the git-backed config value (trusted, server-only Gitea) takes
     * precedence; otherwise fall back to the provider's conventional env var.
     */
    private String apiKey(String providerName) {
        String fromConfig = cfg(String, CfgKey.VOICE.VOICE_LLM_APIKEY.key(), null)
        if (fromConfig?.trim()) {
            return fromConfig.trim()
        }
        String envVar = providerName == 'openai' ? 'OPENAI_API_KEY' : 'ANTHROPIC_API_KEY'
        return System.getenv(envVar)
    }

    private static Map fail(String error, String transcript, String spokenResponse = null) {
        [success: false, error: error, transcript: transcript, action: null,
         peripheralId: null, peripheralName: null, spokenResponse: spokenResponse]
    }

    /** Null-safe, typed read from the git-backed config with a fallback default. */
    private <T> T cfg(Class<T> cls, String key, T defaultValue) {
        try {
            T value = configProvider?.get(cls, key)
            return value != null ? value : defaultValue
        } catch (Exception ignored) {
            return defaultValue
        }
    }
}
