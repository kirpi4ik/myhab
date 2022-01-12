package org.myhab.domain.events

class MQTTTopic {
    String regex
    MQTTTopicType type


    static constraints = {
        table '`mqtt_topics`'
    }
}
