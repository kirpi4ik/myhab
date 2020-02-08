package eu.devexpert.madhouse.domain;

/**
 *
 */
enum TopicName {
    EVT_LIGHT,
    EVT_HEAT,
    EVT_PORT_VALUE_CHANGED,
    EVT_LOG,
    POWER;

    public String id() {
        return name().toLowerCase();
    }

}
