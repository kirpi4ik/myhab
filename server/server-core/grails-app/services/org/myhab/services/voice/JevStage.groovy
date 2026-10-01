package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse

/**
 * Fast-path stage backed by TypeSafe Jev, a cloud "System One" model that answers
 * typed choice questions with calibrated probabilities instead of generating text.
 *
 * <p>One request asks, in parallel: the intent, the target (every catalog
 * peripheral and zone), the on/off action, the scenario and the mower command.
 * The questions are the ones measured in tools/voice-eval/Invoke-JevEval.ps1.</p>
 *
 * <p>Settings: {@code apiKey}, {@code model}, {@code timeoutMs}, {@code catalog}.</p>
 */
class JevStage implements IntentStage {

    static final String NAME = 'jev'
    static final String API_URL = 'https://api.typesafe.ai/v1/systemone'
    static final int MAX_OPTIONS = 255

    private static final Map<String, String> INTENTS = [
        control : 'Turn on/off, toggle, open/close or start/stop a device or a whole area (lights, heating, water valves, gates, sprinklers, switches, ventilation)',
        scenario: 'Run a predefined automation/scenario, named or described',
        query   : 'Ask about a current state or value (temperature, is something on/open?) without changing anything',
        mower   : 'Command the robotic lawn mower: start, stop, pause or resume mowing, or send it to its dock',
        clarify : 'A home command that is ambiguous: it could equally mean several different devices or rooms, so the user must say which',
        other   : 'Not a home-automation request (chit-chat, general knowledge)'
    ]
    private static final Map<String, String> ACTIONS = [
        ON    : 'Turn on, switch on, light up, open (a valve, water, gate), start. Romanian: aprinde, pornește, deschide, dă drumul. Russian: включи, зажги, открой, запусти',
        OFF   : 'Turn off, switch off, close or shut off (a valve, water), stop. Romanian: stinge, oprește, închide, taie. Russian: выключи, погаси, закрой, перекрой, останови',
        TOGGLE: 'Toggle or flip to the opposite state. Romanian: comută, schimbă. Russian: переключи',
        none  : 'No on/off action is requested'
    ]
    private static final Map<String, String> MOWER_ACTIONS = [
        START : 'Start mowing', STOP: 'Stop mowing', PAUSE: 'Pause mowing', RESUME: 'Resume mowing',
        DOCK  : 'Return to the charging dock / go home', none: 'No mower command'
    ]
    /** Which questions an intent's decision is built from. */
    private static final Map<String, List<String>> USED = [
        control: ['intent', 'target', 'action'], query: ['intent', 'target'],
        scenario: ['intent', 'scenario'], mower: ['intent', 'mower_action']
    ]

    @Override
    String name() { NAME }

    @Override
    FastDecision resolve(String transcript, String locale, Map settings) {
        if (!(settings.apiKey as String)?.trim()) {
            throw new IllegalStateException('Jev API key is not set')
        }
        int timeout = (settings.timeoutMs ?: 1500) as int
        Map body = [state: transcript, model: settings.model ?: 'jev-latest', questions: questions(settings.catalog as Map)]
        HttpResponse<String> response = VoiceHttp.INSTANCE.post(API_URL)
                .connectTimeout(timeout)
                .socketTimeout(timeout)
                .header('Authorization', "Bearer ${settings.apiKey}")
                .header('content-type', 'application/json')
                .body(JsonOutput.toJson(body))
                .asString()
        if (response.status >= 400) {
            throw new IllegalStateException("Jev HTTP ${response.status}")
        }
        decide(new JsonSlurper().parseText(response.body).answers as Map)
    }

    /** The choice questions for a catalog. */
    static Map questions(Map catalog) {
        Map<String, String> targets = [:]
        (catalog.peripherals as List<Map>).each { p ->
            String d = "DEVICE: ${p.name} (${p.category})"
            if (p.zones) d += " in ${(p.zones as List).join(', ')}"
            if (p.aliases) d += "; also called ${(p.aliases as List).join(', ')}"
            targets["P${p.id}".toString()] = d
        }
        (catalog.zones as List<Map>).each { z ->
            String d = "AREA (everything in it): ${z.name}; contains ${joinSome(z.peripherals as List, 12)}"
            if (z.aliases) d += "; also called ${(z.aliases as List).join(', ')}"
            targets["Z${z.id}".toString()] = d
        }
        targets.none = 'Nothing in the list matches'
        if (targets.size() > MAX_OPTIONS) {
            throw new IllegalStateException("${targets.size()} target options exceed Jev's ${MAX_OPTIONS}-option limit")
        }

        Map questions = [
            intent      : choice('The text is a spoken request to a home-automation assistant (English, Romanian or Russian; it may contain speech-recognition errors). What kind of request is it?', INTENTS),
            target      : choice('Which single device or area does the user want to control or ask about? Catalog names are Romanian; the user may speak English, Romanian or Russian. Choose an AREA when the user means everything in a place, a DEVICE when they mean one specific device or one kind of device in a room.', targets),
            action      : choice('Which on/off action does the user request?', ACTIONS),
            mower_action: choice('Which robotic lawn mower command is requested?', MOWER_ACTIONS)
        ]
        List<Map> scenarios = catalog.scenarios as List<Map>
        if (scenarios) {
            Map<String, String> options = scenarios.collectEntries { ["S${it.jobId}".toString(), "${it.name}: ${it.description}".toString()] }
            options.none = 'No scenario is requested'
            questions.scenario = choice('Which predefined automation scenario should be run?', options)
        }
        return questions
    }

    /** Decision from Jev's answers; confidence is the minimum over the questions the intent needs. */
    static FastDecision decide(Map answers) {
        String intent = answers.intent?.choice
        List<String> used = USED[intent] ?: ['intent']
        Closure<String> pick = { String q -> String c = answers[q]?.choice; c == 'none' ? null : c }
        new FastDecision(
            stage: NAME,
            intent: intent,
            target: 'target' in used ? pick('target') : null,
            action: 'action' in used ? pick('action') : null,
            scenario: 'scenario' in used ? pick('scenario') : null,
            mowerAction: 'mower_action' in used ? pick('mower_action') : null,
            confidence: used.collect { (answers[it]?.confidence ?: 0) as double }.min())
    }

    private static Map choice(String instructions, Map<String, String> criteria) {
        [type: 'choice', instructions: instructions, criteria: criteria]
    }

    private static String joinSome(List names, int limit) {
        List n = (names ?: []).findAll { it }
        n.size() <= limit ? n.join(', ') : n.take(limit).join(', ') + ", ... (${n.size()} total)"
    }
}
