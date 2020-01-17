package eu.devexpert.madhouse.domain;

/**
 *
 */
public enum TopicName {
    LIGHT,
    PORT_VALUE_CHANGE,
    POWER;

    public String id() {
        return name().toLowerCase();
    }

}
