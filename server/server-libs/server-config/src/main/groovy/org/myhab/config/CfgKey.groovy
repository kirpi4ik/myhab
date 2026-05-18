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

    /**
     * App-level UI configuration. These keys are stored as Configuration rows
     * with entityType=CONFIG and entityId=0, and are exposed to the SPA in a
     * single round trip via the `uiConfigList` GraphQL query. Components read
     * the values from the `useAppConfigStore` Pinia store, hydrated at boot.
     *
     * Values previously lived in `client/web-vue3/.env*` files — migrating
     * here removes the rebuild-and-redeploy cycle every time a device id
     * changes in the database.
     */
    enum UI implements Key {
        UI_DEVICE_DOOR_LOCK_ID('ui.device.door_lock.id'),
        UI_DEVICE_WATER_PUMP_ID('ui.device.water_pump.id'),
        UI_DEVICE_HEAT_PUMP_ID('ui.device.heat_pump.id'),
        UI_DEVICE_ELECTRIC_METER_01_ID('ui.device.electric_meter_01.id'),
        UI_DEVICE_NAVIMOW_ID('ui.device.navimow.id'),
        UI_DEVICE_METEO_STATION_ID('ui.device.meteo_station.id'),
        UI_DEVICE_SOLAR_PLANT_ID('ui.device.solar_plant.id'),
        UI_DEVICE_SOLAR_METER_ID('ui.device.solar_meter.id'),
        UI_ZONE_INT_ID('ui.zone.int.id'),
        UI_ZONE_EXT_ID('ui.zone.ext.id'),
        UI_ZONE_ETAJ_ID('ui.zone.etaj.id'),
        UI_ZONE_PARTER_ID('ui.zone.parter.id'),
        UI_ZONE_LAN_ID('ui.zone.lan.id'),
        UI_ZONE_GARDEN_ID('ui.zone.garden.id'),
        UI_GRAFANA_URL('ui.grafana.url'),
        UI_GRAFANA_DASHBOARD_SOLAR_ID('ui.grafana.dashboard.solar.id'),
        UI_DATE_FORMAT_LONG('ui.date.format.long'),

        def key

        UI(key) {
            this.key = key
        }

        @Override
        def key() {
            return key
        }
    }
}
