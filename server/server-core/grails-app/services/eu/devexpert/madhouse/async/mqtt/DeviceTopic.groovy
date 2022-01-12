package eu.devexpert.madhouse.async.mqtt

interface DeviceTopic {
    String topicByType(TopicTypes topicTypes)

    enum TopicTypes {
        LISTEN,
        READ_SINGLE_VAL,
        WRITE_SINGLE_VAL,
        STAT_IP,
        STAT_PORT,
        STATUS,
    }
}