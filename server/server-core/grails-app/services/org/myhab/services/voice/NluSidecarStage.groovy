package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse

/**
 * Fast-path stage backed by the local NLU sidecar (bridges/voice-nlu): a
 * multilingual sentence encoder with classifier heads trained on this
 * installation's catalog. Answers in tens of milliseconds, without the cloud.
 *
 * <p>Settings: {@code url}, {@code timeoutMs}.</p>
 */
class NluSidecarStage implements IntentStage {

    static final String NAME = 'nlu'

    @Override
    String name() { NAME }

    @Override
    FastDecision resolve(String transcript, String locale, Map settings) {
        HttpResponse<String> response = VoiceHttp.INSTANCE.post("${base(settings)}/resolve")
                .connectTimeout(timeout(settings))
                .socketTimeout(timeout(settings))
                .header('content-type', 'application/json')
                .body(JsonOutput.toJson([text: transcript, locale: locale]))
                .asString()
        if (response.status == 503) {
            return null // heads not trained yet
        }
        if (response.status >= 400) {
            throw new IllegalStateException("NLU sidecar HTTP ${response.status}")
        }
        Map d = new JsonSlurper().parseText(response.body) as Map
        new FastDecision(stage: NAME, intent: d.intent, target: d.target, action: d.action,
                scenario: d.scenario, mowerAction: d.mowerAction,
                confidence: (d.confidence ?: 0) as double, catalogHash: d.catalogHash)
    }

    /** The sidecar's /health document: ready, encoder, catalogHash, training, ... */
    Map health(Map settings) {
        HttpResponse<String> response = VoiceHttp.INSTANCE.get("${base(settings)}/health")
                .connectTimeout(timeout(settings))
                .socketTimeout(timeout(settings))
                .asString()
        if (response.status >= 400) {
            throw new IllegalStateException("NLU sidecar HTTP ${response.status}")
        }
        new JsonSlurper().parseText(response.body) as Map
    }

    /** Hand the sidecar a new catalog; it retrains in the background (a known hash is a no-op). */
    void putCatalog(Map settings, String hash, String catalogJson) {
        HttpResponse<String> response = VoiceHttp.INSTANCE.put("${base(settings)}/catalog")
                .connectTimeout(5000)
                .socketTimeout(30000)
                .header('content-type', 'application/json')
                .body("{\"hash\":${JsonOutput.toJson(hash)},\"catalog\":${catalogJson}}".toString())
                .asString()
        if (response.status >= 400) {
            throw new IllegalStateException("NLU sidecar rejected the catalog: HTTP ${response.status} ${response.body}")
        }
    }

    /** One decision-log record (with the LLM's label, when there is one) for training. */
    void postFeedback(Map settings, Map record) {
        HttpResponse<String> response = VoiceHttp.INSTANCE.post("${base(settings)}/feedback")
                .connectTimeout(2000)
                .socketTimeout(2000)
                .header('content-type', 'application/json')
                .body(JsonOutput.toJson(record))
                .asString()
        if (response.status >= 400) {
            throw new IllegalStateException("NLU sidecar rejected feedback: HTTP ${response.status}")
        }
    }

    private static String base(Map settings) {
        (settings.url as String)?.replaceAll('/+$', '')
    }

    private static int timeout(Map settings) {
        (settings.timeoutMs ?: 300) as int
    }
}
