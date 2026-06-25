package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.EventData
import org.myhab.domain.job.Job
import org.myhab.domain.job.JobState
import org.myhab.services.voice.AnthropicIntentProvider
import org.myhab.services.voice.GoogleTtsProvider
import org.myhab.services.voice.LlmTurn
import org.myhab.services.voice.OpenAiIntentProvider
import org.myhab.services.voice.ToolCall
import org.myhab.services.voice.VoiceIntentProvider
import org.myhab.services.voice.VoiceTools
import org.myhab.services.voice.VoiceTtsProvider

import java.util.concurrent.TimeUnit

/**
 * Voice-assistant pipeline (v2): an agentic tool-use loop that turns a spoken
 * phrase into one or more home-automation actions, answers state questions, and
 * holds a short multi-turn conversation so it can ask for clarification.
 *
 * <p>Flow: build the catalog (peripherals + zones + scenarios, with aliases) →
 * run a bounded loop where the configured {@link VoiceIntentProvider} either
 * calls tools (executed here against existing services) or ends its turn with a
 * spoken reply. Conversation history is kept per {@code sessionId} in Hazelcast
 * (~5-min TTL). When natural TTS is enabled the reply is synthesized and the MP3
 * is returned (base64) for the client to play.</p>
 *
 * <p>Tools: {@code control_entity} (PERIPHERAL/ZONE/PORT on/off/toggle, via the
 * generic {@code evt_switch} → UIMessageService path), {@code run_scenario}
 * (SchedulerService.triggerJob), {@code query_state} (read DevicePort state).</p>
 */
@Slf4j
class VoiceCommandService implements EventPublisher {

    ConfigProvider configProvider
    SchedulerService schedulerService
    HazelcastInstance hazelcastInstance

    /** LLM providers by name; selected via feature.voice.llm.provider. Replaceable in tests. */
    Map<String, VoiceIntentProvider> providers = [
        (new AnthropicIntentProvider().name()): new AnthropicIntentProvider(),
        (new OpenAiIntentProvider().name())   : new OpenAiIntentProvider()
    ]

    /** TTS providers by name; selected via feature.voice.tts.provider. */
    Map<String, VoiceTtsProvider> ttsProviders = [
        (new GoogleTtsProvider().name()): new GoogleTtsProvider()
    ]

    static final String EVT_SWITCH = 'evt_switch'
    static final String DEFAULT_PROVIDER = 'anthropic'
    static final String DEFAULT_TTS_PROVIDER = 'google'
    static final String ALIAS_KEY = 'feature.voice.alias'
    static final String SESSION_MAP = 'voiceSessions'
    static final int SESSION_TTL_SEC = 300
    static final int MAX_ITERATIONS = 6
    static final int MAX_HISTORY = 40
    static final List<String> ACTIONS = ['ON', 'OFF', 'TOGGLE']

    /**
     * Resolve and execute a transcript within a conversation. Always returns a
     * result map shaped like the {@code VoiceCommandResult} GraphQL type; never throws.
     */
    @Transactional
    Map handleTranscript(String transcript, String locale, String sessionId, String username = 'voice') {
        if (!isEnabled()) {
            return fail('Voice feature is disabled', transcript, sessionId)
        }
        if (!transcript?.trim()) {
            return fail('Empty transcript', transcript, sessionId)
        }
        VoiceIntentProvider provider = currentProvider()
        if (!provider) {
            return fail("Unknown LLM provider '${providerName()}'", transcript, sessionId)
        }

        Map catalog = buildCatalog()
        String catalogJson = VoiceTools.catalogJson(catalog)

        String sid = sessionId?.trim() ?: UUID.randomUUID().toString()
        List<Map> messages = loadHistory(sid)
        messages << [role: 'user', text: transcript]

        List<String> actions = []
        boolean stateChanged = false
        String finalText = null

        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                LlmTurn turn = provider.converse(VoiceTools.SYSTEM_PROMPT, catalogJson, messages,
                        VoiceTools.TOOLS, model(provider), apiKey(provider.name()))

                messages << [role      : 'assistant',
                             text      : turn.finalText,
                             toolCalls : turn.toolCalls.collect { [id: it.id, name: it.name, input: it.input] }]
                finalText = turn.finalText ?: finalText

                if (!turn.toolCalls) {
                    break
                }

                List<Map> toolResults = []
                turn.toolCalls.each { ToolCall tc ->
                    Map r
                    try {
                        r = executeTool(tc, catalog, transcript, username)
                    } catch (Exception ex) {
                        // Feed the failure back to the model (instead of aborting the
                        // turn) so it can explain to the user, and so the tool_use /
                        // tool_result pairing stays valid for the next turn.
                        log.warn("Voice tool '${tc.name}' failed: ${ex.message}")
                        r = [content: "ERROR: ${ex.message}", stateChange: false]
                    }
                    if (r.stateChange) stateChanged = true
                    if (r.action) actions << (r.action as String)
                    toolResults << [id: tc.id, name: tc.name, content: r.content]
                }
                messages << [role: 'tool', toolResults: toolResults]
            }
        } catch (Exception e) {
            log.error("Voice loop failed (provider=${provider.name()}) for transcript='${transcript}'", e)
            return fail("Could not complete the command: ${e.message}", transcript, sid)
        }

        if (!finalText) {
            finalText = stateChanged ? 'Done.' : 'Sorry, I could not complete that.'
        }
        // Expect a follow-up only when nothing was actioned and the reply is a
        // question (a clarification) — not after a plain state answer or error.
        boolean awaiting = !stateChanged && (finalText?.trim()?.endsWith('?') ?: false)
        saveHistory(sid, messages)
        log.info("Voice: '${transcript}' -> actions=${actions} awaitingReply=${awaiting}")

        Map result = [
            success      : true,
            error        : null,
            transcript   : transcript,
            spokenResponse: finalText,
            sessionId    : sid,
            awaitingReply: awaiting,
            actions      : actions,
            audioContent : null,
            audioMime    : null
        ]
        attachTts(result, finalText, locale)
        return result
    }

    // ---------------------------------------------------------------- catalog

    /** Build the grounding catalog: controllable peripherals, zones, scenarios. */
    Map buildCatalog() {
        [
            peripherals: DevicePeripheral.list().findAll { it.connectedTo }.collect { p ->
                [id      : p.id,
                 name    : p.name,
                 category: p.category?.name,
                 zones   : (p.zones*.name ?: []) as List,
                 aliases : aliasesFor(EntityType.PERIPHERAL, p.id)]
            }.sort { it.id },
            zones      : Zone.list().collect { z -> [zone: z, names: zonePeripheralNames(z)] }
                .findAll { !it.names.isEmpty() }
                .collect { entry ->
                    [id      : entry.zone.id,
                     name    : entry.zone.name,
                     peripherals: entry.names,
                     aliases : aliasesFor(EntityType.ZONE, entry.zone.id)]
                }.sort { it.id },
            scenarios  : Job.findAllByState(JobState.ACTIVE).findAll { it.scenario }.collect { j ->
                [jobId: j.id, name: j.name, description: j.description]
            }.sort { it.jobId }
        ]
    }

    /** Names of all peripherals in a zone and its child zones (recursive). */
    private List<String> zonePeripheralNames(Zone zone, List<String> acc = []) {
        zone?.peripherals?.each { if (it?.name) acc << it.name }
        zone?.zones?.each { zonePeripheralNames(it, acc) }
        return acc.unique()
    }

    private List<String> aliasesFor(EntityType type, Long id) {
        Configuration.where { entityType == type && entityId == id && key == ALIAS_KEY }.list()
                .collectMany { (it.value ?: '').split(',').collect { s -> s.trim() }.findAll { it } }
                .unique()
    }

    // ----------------------------------------------------------- tool execution

    private Map executeTool(ToolCall tc, Map catalog, String transcript, String username) {
        switch (tc.name) {
            case VoiceTools.CONTROL_ENTITY: return execControl(tc, catalog, transcript, username)
            case VoiceTools.RUN_SCENARIO:   return execScenario(tc, catalog)
            case VoiceTools.QUERY_STATE:    return execQuery(tc)
            default: return [content: "Unknown tool: ${tc.name}", stateChange: false]
        }
    }

    private Map execControl(ToolCall tc, Map catalog, String transcript, String username) {
        String entityType = (tc.input.entityType as String)?.toUpperCase()
        Long id = tc.input.id == null ? null : (tc.input.id as Long)
        String action = (tc.input.action as String)?.toUpperCase()
        if (!(action in ACTIONS)) {
            return [content: "Invalid action '${action}'", stateChange: false]
        }
        if (!validateEntity(entityType, id, catalog)) {
            return [content: "No ${entityType} with id ${id} in the catalog", stateChange: false]
        }
        publishSwitch(entityType, id, action, transcript, username)
        String label = entityLabel(entityType, id, catalog)
        return [content   : "OK: ${action} applied to ${entityType} '${label}'",
                stateChange: true,
                action    : "${action} ${entityType} '${label}'".toString()]
    }

    private Map execScenario(ToolCall tc, Map catalog) {
        Long jobId = tc.input.jobId == null ? null : (tc.input.jobId as Long)
        Map scenario = (catalog.scenarios as List<Map>).find { it.jobId == jobId }
        if (!scenario) {
            return [content: "No scenario with jobId ${jobId} in the catalog", stateChange: false]
        }
        schedulerService.triggerJob(jobId)
        return [content   : "Started scenario '${scenario.name}'",
                stateChange: true,
                action    : "RUN scenario '${scenario.name}'".toString()]
    }

    private Map execQuery(ToolCall tc) {
        String entityType = (tc.input.entityType as String)?.toUpperCase()
        Long id = tc.input.id == null ? null : (tc.input.id as Long)
        List<Map> ports = resolvePorts(entityType, id).collect {
            [name: it.name ?: it.internalRef, state: it.state?.toString(), value: it.value]
        }
        return [content: JsonOutput.toJson([entityType: entityType, id: id, ports: ports]), stateChange: false]
    }

    private boolean validateEntity(String entityType, Long id, Map catalog) {
        if (id == null) return false
        switch (entityType) {
            case 'PERIPHERAL': return (catalog.peripherals as List<Map>).any { it.id == id }
            case 'ZONE':       return (catalog.zones as List<Map>).any { it.id == id }
            case 'PORT':       return DevicePort.get(id) != null
            default:           return false
        }
    }

    private String entityLabel(String entityType, Long id, Map catalog) {
        switch (entityType) {
            case 'PERIPHERAL': return (catalog.peripherals as List<Map>).find { it.id == id }?.name ?: "#${id}"
            case 'ZONE':       return (catalog.zones as List<Map>).find { it.id == id }?.name ?: "#${id}"
            case 'PORT':       return DevicePort.get(id)?.name ?: "#${id}"
            default:           return "#${id}"
        }
    }

    private List<DevicePort> resolvePorts(String entityType, Long id) {
        if (id == null) return []
        switch (entityType) {
            case 'PERIPHERAL':
                DevicePeripheral p = DevicePeripheral.get(id)
                return p ? (p.connectedTo as List<DevicePort>) : []
            case 'PORT':
                DevicePort port = DevicePort.get(id)
                return port ? [port] : []
            case 'ZONE':
                Zone z = Zone.get(id)
                return z ? zonePorts(z, []) : []
            default:
                return []
        }
    }

    private List<DevicePort> zonePorts(Zone zone, List<DevicePort> acc) {
        zone?.peripherals?.each { p -> p?.connectedTo?.each { acc << it } }
        zone?.zones?.each { zonePorts(it, acc) }
        return acc.unique()
    }

    /** Publish the generic switch event consumed by UIMessageService (PERIPHERAL/ZONE/PORT). */
    private void publishSwitch(String entityType, Long id, String action, String transcript, String username) {
        publish(EVT_SWITCH, new EventData().with {
            p0 = EVT_SWITCH
            p1 = entityType
            p2 = id.toString()
            p3 = "Voice assistant: ${username}"
            p4 = action.toLowerCase()
            p5 = JsonOutput.toJson([transcript: transcript])
            p6 = username
            it
        })
    }

    // ------------------------------------------------------------------- TTS

    private void attachTts(Map result, String text, String locale) {
        if (!text || !ttsEnabled()) return
        try {
            VoiceTtsProvider tts = ttsProviders[ttsProviderName()]
            if (!tts) return
            byte[] mp3 = tts.synthesizeMp3(text, locale ?: 'en-US', ttsVoice(locale), ttsApiKey())
            result.audioContent = Base64.encoder.encodeToString(mp3)
            result.audioMime = 'audio/mpeg'
        } catch (Exception e) {
            log.warn("Voice TTS failed (client will fall back to browser speech): ${e.message}")
        }
    }

    // -------------------------------------------------------------- sessions

    private List<Map> loadHistory(String sid) {
        try {
            String json = hazelcastInstance.getMap(SESSION_MAP).get(sid) as String
            if (json) return new JsonSlurper().parseText(json) as List<Map>
        } catch (Exception e) {
            log.warn("Voice session load failed: ${e.message}")
        }
        return []
    }

    private void saveHistory(String sid, List<Map> messages) {
        try {
            List<Map> trimmed = trimHistory(messages)
            hazelcastInstance.getMap(SESSION_MAP).put(sid, JsonOutput.toJson(trimmed), SESSION_TTL_SEC, TimeUnit.SECONDS)
        } catch (Exception e) {
            log.warn("Voice session save failed: ${e.message}")
        }
    }

    /** Cap history; trim only at a plain user-message boundary so tool_use/tool_result pairs stay intact. */
    private static List<Map> trimHistory(List<Map> messages) {
        if (messages.size() <= MAX_HISTORY) return messages
        for (int i = messages.size() - MAX_HISTORY; i < messages.size(); i++) {
            Map m = messages[i]
            if (m.role == 'user' && !m.toolResults) {
                return messages.subList(i, messages.size())
            }
        }
        return messages.subList(messages.size() - MAX_HISTORY, messages.size())
    }

    // ------------------------------------------------------------- config

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

    private String apiKey(String providerName) {
        String fromConfig = cfg(String, CfgKey.VOICE.VOICE_LLM_APIKEY.key(), null)
        if (fromConfig?.trim()) return fromConfig.trim()
        String envVar = providerName == 'openai' ? 'OPENAI_API_KEY' : 'ANTHROPIC_API_KEY'
        return System.getenv(envVar)
    }

    private boolean ttsEnabled() {
        cfg(Boolean, CfgKey.VOICE.VOICE_TTS_ENABLED.key(), false)
    }

    private String ttsProviderName() {
        cfg(String, CfgKey.VOICE.VOICE_TTS_PROVIDER.key(), DEFAULT_TTS_PROVIDER)?.toLowerCase()
    }

    private String ttsApiKey() {
        String fromConfig = cfg(String, CfgKey.VOICE.VOICE_TTS_APIKEY.key(), null)
        if (fromConfig?.trim()) return fromConfig.trim()
        return System.getenv('GOOGLE_TTS_API_KEY')
    }

    private String ttsVoice(String locale) {
        boolean ro = (locale ?: '').toLowerCase().startsWith('ro')
        cfg(String, (ro ? CfgKey.VOICE.VOICE_TTS_VOICE_RO : CfgKey.VOICE.VOICE_TTS_VOICE_EN).key(), null)
    }

    private static Map fail(String error, String transcript, String sessionId) {
        [success      : false, error: error, transcript: transcript, spokenResponse: null,
         sessionId    : sessionId, awaitingReply: false, actions: [], audioContent: null, audioMime: null]
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
