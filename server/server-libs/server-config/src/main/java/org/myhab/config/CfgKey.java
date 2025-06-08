package org.myhab.config;

public class CfgKey {
    public interface Key {
        String key();
    }

    enum DEVICE implements Key {
        DEVICE_HTTP_SYNC_SUPPORTED("cfg.key.device.http.sync.supported"),
        DEVICE_OAUTH_ACCESS_USER("cfg.key.device.oauth.access_user"),
        DEVICE_OAUTH_ACCESS_PASSWD("cfg.key.device.oauth.access_passwd"),
        DEVICE_OAUTH_ACCESS_TOKEN("cfg.key.device.oauth.access_token"),
        DEVICE_OAUTH_REFRESH_TOKEN("cfg.key.device.oauth.refresh_token"),
        DEVICE_MQTT_SYNC_SUPPORTED("cfg.key.device.mqtt.sync.supported"),
        DEVICE_ADMIN_PORT_AUTO_IMPORT("cfg.key.device.admin.port.autoimport");

        String key;

        DEVICE(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return this.key;
        }
    }
}
