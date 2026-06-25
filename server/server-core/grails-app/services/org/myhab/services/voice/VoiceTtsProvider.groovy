package org.myhab.services.voice

/**
 * Text-to-speech vendor behind a neutral API. Returns raw MP3 bytes so the SPA
 * can play a natural neural voice instead of the browser's OS voices (which read
 * Romanian awkwardly). Selected by {@code feature.voice.tts.provider}.
 */
interface VoiceTtsProvider {

    /** Lower-case provider id matching {@code feature.voice.tts.provider}. */
    String name()

    /**
     * @param text         text to speak
     * @param languageCode BCP-47 code ('ro-RO' / 'en-US'); bare 'ro'/'en' accepted
     * @param voiceName    optional vendor voice name (null = vendor default for the language)
     * @return MP3 audio bytes
     */
    byte[] synthesizeMp3(String text, String languageCode, String voiceName, String apiKey)
}
