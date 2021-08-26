package eu.devexpert.madhouse.domain;

/**
 *
 */
enum TopicName {
    EVT_LIGHT,
    EVT_HEAT,
    EVT_PRESENCE,
    EVT_PORT_VALUE_CHANGED,
    EVT_LOG,
    EVT_DEVICE_STATUS,
    EVT_INTERCOM_DOOR_LOCK,
    POWER,;

    String id() {
        return name().toLowerCase();
    }

    static TopicName byOrder(Integer order) {
        values()[order]
    }

}
