package org.myhab.domain.events;

/**
 *
 */
enum TopicName {
    EVT_LIGHT,
    EVT_HEAT,
    EVT_PRESENCE,
    EVT_PORT_VALUE_CHANGED,
    EVT_PORT_VALUE_PERSISTED,
    EVT_LOG,
    EVT_DEVICE_STATUS,
    EVT_INTERCOM_DOOR_LOCK,
    EVT_DEVICE_PUSH,
    EVT_MQTT_PORT_VALUE_CHANGED,
    EVT_ASYNC_PORT_VALUE_CHANGED,
    EVT_UI_UPDATE_PORT_VALUE,
    EVT_CFG_VALUE_CHANGED,
    /**
     * Fired when an entry in the git-backed ConfigProvider changes (via the
     * `appConfigUpdate` GraphQL mutation). Distinct from EVT_CFG_VALUE_CHANGED
     * which is for DB-backed Configuration rows (per-entity config); this one
     * is for global app config (mqtt.*, telegram.*, ui.*, etc.). The SPA
     * listens for this to keep its `useAppConfigStore` in sync without
     * reloading.
     */
    EVT_APP_CFG_VALUE_CHANGED,
    /**
     * Fired when a DashboardScreen (mobile /wui screen definition) is created,
     * updated or deleted — layout saves, reorders, background uploads, legacy
     * imports. Open viewers listen for this to refetch screens without a
     * page reload.
     */
    EVT_DASHBOARD_SCREEN_CHANGED,
    EVT_STAT_VALUE_CHANGED,
    POWER,
    /**
     * Generic in-app notification raised by any device via `myhab/<source>/notify`.
     * p2 = source, p5 = raw JSON envelope. Deliberately off the port-value path:
     * PortValueService skips unchanged values, so repeated identical events would be lost.
     *
     * New entries go at the end — byOrder(Integer) is values()[order] and is called
     * with ordinals supplied by external device URLs.
     */
    EVT_USER_NOTIFICATION;

    String id() {
        return name().toLowerCase();
    }

    static TopicName byOrder(Integer order) {
        values()[order]
    }

}
