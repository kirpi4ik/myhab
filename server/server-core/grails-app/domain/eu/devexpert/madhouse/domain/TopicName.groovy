package eu.devexpert.madhouse.domain;

/**
 *
 */
enum TopicName {
    LIGHT,
    PORT_VALUE_CHANGE,
    POWER;

    public String id() {
        return name().toLowerCase();
    }

}
