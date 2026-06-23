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
    // in the git-backed ConfigProvider (a trusted, server-only Gitea instance).
    // The API key is read from VOICE_LLM_APIKEY first and falls back to the
    // provider's env var (ANTHROPIC_API_KEY / OPENAI_API_KEY) when absent.
    // Code-side defaults are applied in VoiceCommandService.
    enum VOICE implements Key {
        VOICE_ENABLED('feature.voice.enabled'),
        VOICE_LLM_PROVIDER('feature.voice.llm.provider'),
        VOICE_LLM_MODEL('feature.voice.llm.model'),
        VOICE_LLM_APIKEY('feature.voice.llm.apikey')

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
