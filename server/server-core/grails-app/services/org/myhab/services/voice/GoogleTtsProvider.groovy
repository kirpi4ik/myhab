package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

/**
 * {@link VoiceTtsProvider} backed by Google Cloud Text-to-Speech, which has
 * strong neural ro-RO / en-US voices. Uses the isolated {@link VoiceHttp}
 * instance (immune to the global 2s Unirest timeout).
 *
 * <p>Cloud TTS does not accept API keys, so the {@code credential} must be a
 * service-account JSON key; {@link GoogleAuth} exchanges it for a short-lived
 * OAuth2 access token sent as a Bearer token.</p>
 */
@Slf4j
class GoogleTtsProvider implements VoiceTtsProvider {

    static final String API_URL = 'https://texttospeech.googleapis.com/v1/text:synthesize'

    @Override
    String name() { 'google' }

    @Override
    byte[] synthesizeMp3(String text, String languageCode, String voiceName, String credential) {
        if (!credential?.trim()) {
            throw new IllegalStateException('Google TTS service account is not set')
        }
        String accessToken = GoogleAuth.accessToken(credential)
        String lang = normalizeLang(languageCode)
        Map voice = [languageCode: lang]
        if (voiceName?.trim()) {
            voice.name = voiceName.trim()
        }
        Map body = [
            input      : [text: text],
            voice      : voice,
            audioConfig: [audioEncoding: 'MP3']
        ]

        HttpResponse<String> response
        try {
            response = VoiceHttp.INSTANCE.post(API_URL)
                    .header('Authorization', "Bearer ${accessToken}")
                    .header('content-type', 'application/json')
                    .body(JsonOutput.toJson(body))
                    .asString()
        } catch (Exception ex) {
            throw new IllegalStateException("Google TTS transport error: ${ex.message}", ex)
        }
        if (response.status >= 400) {
            log.warn("Google TTS HTTP ${response.status}: ${response.body}")
            throw new IllegalStateException("Google TTS HTTP ${response.status}")
        }
        Map env = new JsonSlurper().parseText(response.body ?: '{}') as Map
        String audio = env.audioContent as String
        if (!audio) {
            throw new IllegalStateException('Google TTS returned no audioContent')
        }
        return Base64.decoder.decode(audio)
    }

    /** 'ro' -> 'ro-RO', 'en' -> 'en-US'; pass through anything already regioned. */
    private static String normalizeLang(String code) {
        if (!code) return 'en-US'
        if (code.contains('-')) return code
        switch (code.toLowerCase()) {
            case 'ro': return 'ro-RO'
            case 'en': return 'en-US'
            default: return code
        }
    }
}
