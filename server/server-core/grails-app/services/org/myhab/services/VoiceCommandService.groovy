package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.myhab.config.CfgKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.EventData
import org.myhab.domain.job.Job
import org.myhab.domain.job.JobState
import org.myhab.services.dsl.action.NavimowCommandService
import org.myhab.services.voice.AnthropicIntentProvider
import org.myhab.services.voice.FastDecision
import org.myhab.services.voice.GoogleTtsProvider
import org.myhab.services.voice.IntentStage
import org.myhab.services.voice.JevStage
import org.myhab.services.voice.LlmTurn
import org.myhab.services.voice.NluSidecarStage
import org.myhab.services.voice.OpenAiIntentProvider
import org.myhab.services.voice.ToolCall
import org.myhab.services.voice.VoiceIntentProvider
import org.myhab.services.voice.VoiceReplies
import org.myhab.services.voice.VoiceTools
import org.myhab.services.voice.VoiceTtsProvider
import org.springframework.beans.factory.annotation.Autowired

import java.security.MessageDigest
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
 *
 * <p>Fast path: before the loop, the enabled {@link IntentStage}s (the local NLU
 * sidecar, then Jev) are tried in order. A stage's decision is executed through the same
 * tools, with a templated reply, when it is actionable, clears the stage's gate
 * and passes the guards; anything else falls through to the LLM. In shadow mode a
 * stage only logs what it would have done.</p>
 */
@Slf4j
class VoiceCommandService implements EventPublisher {

    ConfigProvider configProvider
    SchedulerService schedulerService
    HazelcastInstance hazelcastInstance
    NavimowCommandService navimowCommandService

    /** LLM providers by name; selected via feature.voice.llm.provider. Replaceable in tests. */
    Map<String, VoiceIntentProvider> providers = [
        (new AnthropicIntentProvider().name()): new AnthropicIntentProvider(),
        (new OpenAiIntentProvider().name())   : new OpenAiIntentProvider()
    ]

    /** TTS providers by name; selected via feature.voice.tts.provider. */
    Map<String, VoiceTtsProvider> ttsProviders = [
        (new GoogleTtsProvider().name()): new GoogleTtsProvider()
    ]

    /** Fast-path stages by name, tried in {@link #STAGE_ORDER}. Replaceable in tests. */
    Map<String, IntentStage> stages = [
        (NluSidecarStage.NAME): new NluSidecarStage(),
        (JevStage.NAME)       : new JevStage()
    ]

    @Autowired(required = false)
    MeterRegistry meterRegistry

    static final String EVT_SWITCH = 'evt_switch'
    static final String DEFAULT_PROVIDER = 'anthropic'
    static final String DEFAULT_TTS_PROVIDER = 'google'
    static final String ALIAS_KEY = 'feature.voice.alias'
    static final String SESSION_MAP = 'voiceSessions'
    static final int SESSION_TTL_SEC = 300
    static final int MAX_ITERATIONS = 6
    static final int MAX_HISTORY = 40
    static final List<String> ACTIONS = ['ON', 'OFF', 'TOGGLE']
    static final List<String> STAGE_ORDER = [NluSidecarStage.NAME, JevStage.NAME]
    static final String DEFAULT_FAST_INTENTS = 'control,scenario,mower'
    static final double DEFAULT_ZONE_OFF_MIN_CONFIDENCE = 0.95d
    static final Map<String, Map> STAGE_DEFAULTS = [
        (NluSidecarStage.NAME): [enabled: CfgKey.VOICE.VOICE_NLU_ENABLED, mode: CfgKey.VOICE.VOICE_NLU_MODE,
                                 // Suggested gate of the sidecar's default encoder (e5-small); 0.9 for minilm.
                                 gate   : CfgKey.VOICE.VOICE_NLU_GATE, defaultGate: 0.6d],
        (JevStage.NAME)       : [enabled: CfgKey.VOICE.VOICE_JEV_ENABLED, mode: CfgKey.VOICE.VOICE_JEV_MODE,
                                 gate   : CfgKey.VOICE.VOICE_JEV_GATE, defaultGate: 0.7d]
    ]

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
        // An answer to the assistant's own question belongs to that conversation.
        boolean followUp = awaitingReply(messages)
        messages << [role: 'user', text: transcript]

        List<Map> shadow = []
        if (!followUp) {
            Map fast = runFastPath(transcript, locale, catalog, username, shadow)
            if (fast) {
                return completeFast(fast, transcript, locale, sid, messages)
            }
        }

        List<String> actions = []
        boolean stateChanged = false
        String finalText = null
        ToolCall firstToolCall = null
        long llmStarted = System.nanoTime()

        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                LlmTurn turn = provider.converse(VoiceTools.SYSTEM_PROMPT, catalogJson, messages,
                        VoiceTools.TOOLS, model(provider), apiKey(provider.name()))

                messages << [role      : 'assistant',
                             text      : turn.finalText,
                             toolCalls : turn.toolCalls.collect { [id: it.id, name: it.name, input: it.input] }]
                finalText = turn.finalText ?: finalText
                firstToolCall = firstToolCall ?: turn.toolCalls?.find()

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
            recordStage('llm', 'error', llmStarted)
            log.error("Voice loop failed (provider=${provider.name()}) for transcript='${transcript}'", e)
            return fail("Could not complete the command: ${e.message}", transcript, sid)
        }
        recordStage('llm', 'accepted', llmStarted)
        countResolved('llm')
        logShadow(shadow, firstToolCall, transcript)

        if (!finalText) {
            finalText = stateChanged ? 'Done.' : 'Sorry, I could not complete that.'
        }
        // Expect a follow-up only when nothing was actioned and the reply is a
        // question (a clarification) — not after a plain state answer or error.
        boolean awaiting = !stateChanged && (finalText?.trim()?.endsWith('?') ?: false)
        saveHistory(sid, messages)
        log.info("Voice: '${transcript}' -> actions=${actions} awaitingReply=${awaiting} resolvedBy=llm")

        Map result = [
            success      : true,
            error        : null,
            transcript   : transcript,
            spokenResponse: finalText,
            sessionId    : sid,
            awaitingReply: awaiting,
            actions      : actions,
            resolvedBy   : 'llm',
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
            }.sort { it.jobId },
            mowers     : Device.findAllByModel(DeviceModel.NAVIMOW_SEGWAY).collect { d ->
                [deviceId: d.id, name: d.name ?: d.code]
            }.sort { it.deviceId }
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

    // ------------------------------------------------------------- fast path

    /**
     * Try the enabled stages in order. Returns [decision, result, label] for the first
     * decision that was executed, or null to fall through to the LLM. Shadow-mode
     * decisions are collected into {@code shadow} and never executed.
     */
    private Map runFastPath(String transcript, String locale, Map catalog, String username, List<Map> shadow) {
        for (String name : STAGE_ORDER) {
            IntentStage stage = stages[name]
            if (!stage || !stageEnabled(name)) continue
            long started = System.nanoTime()
            FastDecision decision
            try {
                decision = stage.resolve(transcript, locale, stageSettings(name) + [catalog: catalog])
            } catch (Exception e) {
                recordStage(name, 'error', started)
                log.warn("Voice stage '${name}' failed, falling through: ${e.message}")
                continue
            }
            boolean accepted = decision != null && acceptable(decision, name, catalog)
            if (stageMode(name) == 'shadow') {
                recordStage(name, accepted ? 'shadow-accept' : 'shadow-fallthrough', started)
                if (decision) shadow << [decision: decision, accepted: accepted]
                continue
            }
            if (!accepted) {
                recordStage(name, 'fallthrough', started)
                continue
            }
            Map r
            try {
                r = executeTool(toToolCall(decision, catalog), catalog, transcript, username)
            } catch (Exception e) {
                recordStage(name, 'error', started)
                log.warn("Voice stage '${name}' decision ${decision.key()} failed to execute, falling through: ${e.message}")
                continue
            }
            if (!r.stateChange) {
                recordStage(name, 'fallthrough', started)
                continue
            }
            recordStage(name, 'accepted', started)
            return [decision: decision, result: r, label: fastLabel(decision, catalog)]
        }
        return null
    }

    private boolean acceptable(FastDecision d, String stageName, Map catalog) {
        if (!(d.intent in fastIntents()) || d.confidence < stageGate(stageName)) return false
        switch (d.intent) {
            case 'control':
                if (!d.target || !(d.action in ACTIONS)) return false
                // A zone-wide OFF can cut devices the user did not mean (water, network).
                return !(d.target.startsWith('Z') && d.action == 'OFF' && d.confidence < zoneOffMinConfidence())
            case 'scenario':
                return d.scenario != null
            case 'mower':
                return d.mowerAction != null && (catalog.mowers as List).size() == 1
            default:
                return false
        }
    }

    private static ToolCall toToolCall(FastDecision d, Map catalog) {
        switch (d.intent) {
            case 'control':
                return new ToolCall(id: d.stage, name: VoiceTools.CONTROL_ENTITY,
                        input: [entityType: d.target.startsWith('Z') ? 'ZONE' : 'PERIPHERAL', id: idOf(d.target), action: d.action])
            case 'scenario':
                return new ToolCall(id: d.stage, name: VoiceTools.RUN_SCENARIO, input: [jobId: idOf(d.scenario)])
            default:
                return new ToolCall(id: d.stage, name: VoiceTools.MOWER_COMMAND,
                        input: [deviceId: (catalog.mowers as List<Map>)[0].deviceId, action: d.mowerAction])
        }
    }

    /** Catalog id from a stage key such as P12 / Z7 / S3; null when malformed (then validation rejects it). */
    private static Long idOf(String key) {
        key?.length() > 1 && key.substring(1).isLong() ? key.substring(1) as Long : null
    }

    private String fastLabel(FastDecision d, Map catalog) {
        switch (d.intent) {
            case 'control':  return entityLabel(d.target.startsWith('Z') ? 'ZONE' : 'PERIPHERAL', idOf(d.target), catalog)
            case 'scenario': return (catalog.scenarios as List<Map>).find { it.jobId == idOf(d.scenario) }?.name
            default:         return (catalog.mowers as List<Map>)[0]?.name
        }
    }

    private Map completeFast(Map fast, String transcript, String locale, String sid, List<Map> messages) {
        FastDecision d = fast.decision as FastDecision
        String reply = VoiceReplies.confirm(d, fast.label as String, locale)
        // A plain text turn (no tool pair) so a later LLM turn has the context.
        messages << [role: 'assistant', text: reply]
        saveHistory(sid, messages)
        countResolved(d.stage)
        log.info("Voice: '${transcript}' -> actions=[${fast.result.action}] resolvedBy=${d.stage} confidence=${d.confidence}")
        Map result = [
            success       : true,
            error         : null,
            transcript    : transcript,
            spokenResponse: reply,
            sessionId     : sid,
            awaitingReply : false,
            actions       : [fast.result.action as String],
            resolvedBy    : d.stage,
            audioContent  : null,
            audioMime     : null
        ]
        attachTts(result, reply, locale)
        return result
    }

    /** True when the last turn was the assistant asking the user something. */
    private static boolean awaitingReply(List<Map> history) {
        Map last = history ? history[-1] : null
        last?.role == 'assistant' && !last.toolCalls && ((last.text as String)?.trim()?.endsWith('?') ?: false)
    }

    /** Shadow mode: compare what each stage would have done with the LLM's first tool call. */
    private void logShadow(List<Map> shadow, ToolCall llmCall, String transcript) {
        if (!shadow) return
        String llmKey = llmDecisionKey(llmCall)
        shadow.each { Map s ->
            FastDecision d = s.decision as FastDecision
            log.info("Voice shadow: stage=${d.stage} fast=${d.key()} confidence=${d.confidence} accepted=${s.accepted} " +
                    "llm=${llmKey} agree=${d.key() == llmKey} transcript='${transcript}'")
        }
    }

    /** The LLM's first tool call in the stage key form, e.g. control|Z7|OFF; 'none' when it called no tool. */
    private static String llmDecisionKey(ToolCall tc) {
        if (!tc) return 'none'
        def entity = { -> (tc.input.entityType == 'ZONE' ? 'Z' : tc.input.entityType == 'PORT' ? 'PORT' : 'P') + tc.input.id }
        switch (tc.name) {
            case VoiceTools.CONTROL_ENTITY: return "control|${entity()}|${tc.input.action}"
            case VoiceTools.QUERY_STATE:    return "query|${entity()}"
            case VoiceTools.RUN_SCENARIO:   return "scenario|S${tc.input.jobId}"
            case VoiceTools.MOWER_COMMAND:  return "mower|${tc.input.action}"
            default:                        return tc.name
        }
    }

    /**
     * Send the current catalog to the NLU sidecar when its trained heads are for a
     * different one. Called by NluCatalogSyncJob; the sidecar retrains in the background.
     */
    void syncNluCatalog() {
        if (!stageEnabled(NluSidecarStage.NAME)) return
        NluSidecarStage nlu = stages[NluSidecarStage.NAME] as NluSidecarStage
        if (!nlu) return
        String catalogJson = VoiceTools.catalogJson(buildCatalog())
        String hash = MessageDigest.getInstance('SHA-256').digest(catalogJson.getBytes('UTF-8')).encodeHex().toString()
        Map settings = stageSettings(NluSidecarStage.NAME) + [timeoutMs: 5000]
        Map health = nlu.health(settings)
        if (health.catalogHash == hash) return
        nlu.putCatalog(settings, hash, catalogJson)
        log.info("Voice NLU: sent catalog ${hash.take(12)} (sidecar had ${(health.catalogHash as String)?.take(12)}, encoder ${health.encoder})")
    }

    private void recordStage(String stage, String outcome, long startedNanos) {
        if (!meterRegistry) return
        Timer.builder('voice.stage.duration').tag('stage', stage).tag('outcome', outcome)
                .register(meterRegistry).record(System.nanoTime() - startedNanos, TimeUnit.NANOSECONDS)
    }

    private void countResolved(String stage) {
        meterRegistry?.counter('voice.resolved', 'stage', stage)?.increment()
    }

    // ----------------------------------------------------------- tool execution

    private Map executeTool(ToolCall tc, Map catalog, String transcript, String username) {
        switch (tc.name) {
            case VoiceTools.CONTROL_ENTITY: return execControl(tc, catalog, transcript, username)
            case VoiceTools.RUN_SCENARIO:   return execScenario(tc, catalog, username)
            case VoiceTools.QUERY_STATE:    return execQuery(tc)
            case VoiceTools.MOWER_COMMAND:  return execMower(tc, catalog)
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

    private Map execScenario(ToolCall tc, Map catalog, String username = 'voice') {
        Long jobId = tc.input.jobId == null ? null : (tc.input.jobId as Long)
        Map scenario = (catalog.scenarios as List<Map>).find { it.jobId == jobId }
        if (!scenario) {
            return [content: "No scenario with jobId ${jobId} in the catalog", stateChange: false]
        }
        schedulerService.triggerJob(jobId, username)
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

    private Map execMower(ToolCall tc, Map catalog) {
        Long deviceId = tc.input.deviceId == null ? null : (tc.input.deviceId as Long)
        String action = (tc.input.action as String)?.toUpperCase()
        Map mower = (catalog.mowers as List<Map>).find { it.deviceId == deviceId }
        if (!mower) {
            return [content: "No mower with deviceId ${deviceId} in the catalog", stateChange: false]
        }
        // Throws NavimowApiException on rejection — caught by the loop and fed back to the model.
        navimowCommandService.execute([deviceId: deviceId, action: action])
        return [content   : "Mower '${mower.name}': ${action} sent",
                stateChange: true,
                action    : "MOWER ${action} '${mower.name}'".toString()]
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
        long started = System.nanoTime()
        try {
            VoiceTtsProvider tts = ttsProviders[ttsProviderName()]
            if (!tts) return
            byte[] mp3 = tts.synthesizeMp3(text, locale ?: 'en-US', ttsVoice(locale), ttsApiKey())
            result.audioContent = Base64.encoder.encodeToString(mp3)
            result.audioMime = 'audio/mpeg'
            recordStage('tts', 'accepted', started)
        } catch (Exception e) {
            recordStage('tts', 'error', started)
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

    private boolean stageEnabled(String stage) {
        cfg(Boolean, (STAGE_DEFAULTS[stage].enabled as CfgKey.VOICE).key(), false)
    }

    /** 'active' executes decisions; anything else is shadow (resolve and log only). */
    private String stageMode(String stage) {
        cfg(String, (STAGE_DEFAULTS[stage].mode as CfgKey.VOICE).key(), 'shadow')?.trim()?.toLowerCase() == 'active' ? 'active' : 'shadow'
    }

    private double stageGate(String stage) {
        cfg(Double, (STAGE_DEFAULTS[stage].gate as CfgKey.VOICE).key(), STAGE_DEFAULTS[stage].defaultGate as Double)
    }

    private Map stageSettings(String stage) {
        switch (stage) {
            case NluSidecarStage.NAME:
                return [url      : cfg(String, CfgKey.VOICE.VOICE_NLU_URL.key(), 'http://localhost:8090'),
                        timeoutMs: cfg(Integer, CfgKey.VOICE.VOICE_NLU_TIMEOUT_MS.key(), 300)]
            case JevStage.NAME:
                String key = cfg(String, CfgKey.VOICE.VOICE_JEV_APIKEY.key(), null)?.trim()
                return [apiKey   : key ?: System.getenv('JEV_API_KEY'),
                        model    : cfg(String, CfgKey.VOICE.VOICE_JEV_MODEL.key(), 'jev-latest'),
                        timeoutMs: cfg(Integer, CfgKey.VOICE.VOICE_JEV_TIMEOUT_MS.key(), 1500)]
            default:
                return [:]
        }
    }

    private List<String> fastIntents() {
        cfg(String, CfgKey.VOICE.VOICE_FAST_INTENTS.key(), DEFAULT_FAST_INTENTS)
                .split(',').collect { it.trim().toLowerCase() }.findAll { it }
    }

    private double zoneOffMinConfidence() {
        cfg(Double, CfgKey.VOICE.VOICE_FAST_ZONE_OFF_MIN_CONFIDENCE.key(), DEFAULT_ZONE_OFF_MIN_CONFIDENCE)
    }

    private static Map fail(String error, String transcript, String sessionId) {
        [success      : false, error: error, transcript: transcript, spokenResponse: null,
         sessionId    : sessionId, awaitingReply: false, actions: [], resolvedBy: null, audioContent: null, audioMime: null]
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
