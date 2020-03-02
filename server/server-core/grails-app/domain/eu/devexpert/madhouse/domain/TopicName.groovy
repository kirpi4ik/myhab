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
    POWER;

    String id() {
        return name().toLowerCase();
    }

    static TopicName byOrder(Integer order) {
        values()[order]
    }

}
