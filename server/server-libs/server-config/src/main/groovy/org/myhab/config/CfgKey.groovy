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
}
