package org.myhab.config

class CfgKey {
    interface Key {
        key()
    }

    enum DEVICE implements Key {
        DEVICE_HTTP_SYNC_SUPPORTED('cfg.key.device.http.sync.supported'),
        DEVICE_OAUTH_ACCESS_USER('cfg.key.device.oauth.access_user'),
        DEVICE_OAUTH_ACCESS_PASSWD('cfg.key.device.oauth.access_passwd'),
        DEVICE_OAUTH_ACCESS_TOKEN('cfg.key.device.oauth.access_token'),
        DEVICE_OAUTH_REFRESH_TOKEN('cfg.key.device.oauth.refresh_token'),
        DEVICE_MQTT_SYNC_SUPPORTED('cfg.key.device.mqtt.sync.supported'),
        DEVICE_ADMIN_PORT_AUTO_IMPORT('cfg.key.device.admin.port.autoimport'),
        // Per-device Navimow / Segway integration. base_url varies by region
        // (e.g. EU vs CN cloud); device_id is the mower's id returned by
        // /openapi/smarthome/authList. Access token reuses DEVICE_OAUTH_ACCESS_TOKEN.
        DEVICE_NAVIMOW_API_BASE_URL('cfg.key.device.navimow.api.base_url'),
        DEVICE_NAVIMOW_DEVICE_ID('cfg.key.device.navimow.device.id'),

        def key

        DEVICE(key) {
            this.key = key
        }

        @Override
        def key() {
            return key
        }
    }

    // Voice-assistant feature: toggle, LLM provider/model and (optionally) the
    // API key used to map a spoken transcript to a peripheral + action. Stored
    // in the git-backed ConfigProvider (a trusted, server-only git repository).
    // The API key is read from VOICE_LLM_APIKEY first and falls back to the
    // provider's env var (ANTHROPIC_API_KEY / OPENAI_API_KEY) when absent.
    // Code-side defaults are applied in VoiceCommandService.
    enum VOICE implements Key {
        VOICE_ENABLED('feature.voice.enabled'),
        VOICE_LLM_PROVIDER('feature.voice.llm.provider'),
        VOICE_LLM_MODEL('feature.voice.llm.model'),
        VOICE_LLM_APIKEY('feature.voice.llm.apikey'),
        // Natural neural text-to-speech for the spoken response. The credential
        // is config-first with env fallback (GOOGLE_TTS_API_KEY). NOTE: Google
        // Cloud TTS rejects API keys — VOICE_TTS_APIKEY must hold a *service
        // account JSON key* (exchanged for an OAuth2 token by GoogleAuth).
        VOICE_TTS_ENABLED('feature.voice.tts.enabled'),
        VOICE_TTS_PROVIDER('feature.voice.tts.provider'),
        VOICE_TTS_APIKEY('feature.voice.tts.apikey'),
        VOICE_TTS_VOICE_RO('feature.voice.tts.voice.ro'),
        VOICE_TTS_VOICE_EN('feature.voice.tts.voice.en'),
        // Fast path in front of the LLM: the local NLU sidecar (bridges/voice-nlu).
        // mode = shadow (resolve and log only; the LLM still acts) | active.
        VOICE_NLU_ENABLED('feature.voice.nlu.enabled'),
        VOICE_NLU_MODE('feature.voice.nlu.mode'),
        VOICE_NLU_URL('feature.voice.nlu.url'),
        VOICE_NLU_GATE('feature.voice.nlu.gate'),
        VOICE_NLU_TIMEOUT_MS('feature.voice.nlu.timeoutMs'),
        // Send each command's decisions and the LLM's label to the sidecar, which
        // keeps them (locally) and learns from them. Default on while the stage is on.
        VOICE_NLU_FEEDBACK('feature.voice.nlu.feedback'),
        // Second fast-path stage: TypeSafe Jev (cloud). Same mode/gate semantics as
        // the NLU stage. The API key falls back to the JEV_API_KEY env var.
        VOICE_JEV_ENABLED('feature.voice.jev.enabled'),
        VOICE_JEV_MODE('feature.voice.jev.mode'),
        VOICE_JEV_GATE('feature.voice.jev.gate'),
        VOICE_JEV_TIMEOUT_MS('feature.voice.jev.timeoutMs'),
        VOICE_JEV_MODEL('feature.voice.jev.model'),
        VOICE_JEV_APIKEY('feature.voice.jev.apikey'),
        // Intents the fast path may execute (comma-separated), and the stricter
        // confidence a zone-wide OFF needs before the fast path acts on it.
        VOICE_FAST_INTENTS('feature.voice.fast.intents'),
        VOICE_FAST_ZONE_OFF_MIN_CONFIDENCE('feature.voice.fast.zoneOffMinConfidence')

        def key

        VOICE(key) {
            this.key = key
        }

        @Override
        def key() {
            return key
        }
    }

    // Global QR-code feature: toggle, content template and rendering options.
    // Stored in the git-backed ConfigProvider (overrides.yaml). Code-side
    // defaults are applied in LabelService when a key is absent.
    enum QR implements Key {
        QR_ENABLED('feature.qr.enabled'),
        QR_CONTENT_TEMPLATE('feature.qr.content.template'),
        QR_POSITION('feature.qr.position'),
        QR_SIZE('feature.qr.size')

        def key

        QR(key) {
            this.key = key
        }

        @Override
        def key() {
            return key
        }
    }

}
