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
    EVT_STAT_VALUE_CHANGED,
    POWER;

    String id() {
        return name().toLowerCase();
    }

    static TopicName byOrder(Integer order) {
        values()[order]
    }

}
