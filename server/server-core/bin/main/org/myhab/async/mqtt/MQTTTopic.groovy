package org.myhab.async.mqtt

enum MQTTTopic {
    class COMMON implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return 'myhab/#'
                case TopicTypes.READ_SINGLE_VAL:
                    return '(\\w+)/([0-9]+)'
                case TopicTypes.WRITE_SINGLE_VAL:
                    return '$map.deviceCode/cmd'
                case TopicTypes.STAT_IP:
                    return '$map.deviceCode/address_ip/state'
                case TopicTypes.STAT_PORT:
                    return '$map.deviceCode/address_port/state'
                case TopicTypes.STATUS:
                    return '$map.deviceCode/status'
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }

    class MEGA implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return '$map.deviceCode/#'
                case TopicTypes.READ_SINGLE_VAL:
                    return '(\\w+)/([0-9]+)'
                case TopicTypes.WRITE_SINGLE_VAL:
                    return '$map.deviceCode/cmd'
                case TopicTypes.STAT_IP:
                    return '$map.deviceCode/address_ip/state'
                case TopicTypes.STAT_PORT:
                    return '$map.deviceCode/address_port/state'
                case TopicTypes.STATUS:
                    return '$map.deviceCode/status'
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }

    class ESP implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return 'myhab/#'
                case TopicTypes.READ_SINGLE_VAL:
                    return 'myhab/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/state'
                case TopicTypes.WRITE_SINGLE_VAL:
                    return 'myhab/$map.deviceCode/$map.portType/$map.portCode/cmd'
                case TopicTypes.STAT_IP:
                    return 'myhab/$map.deviceCode/sensor/esp_ip_address/state'
                case TopicTypes.STAT_PORT:
                    return 'myhab/$map.deviceCode/sensor/esp_ip_address/state'
                case TopicTypes.STATUS:
                    return 'myhab/(\\w+|_+)/status'
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }

    class NIBE implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return 'myhab/#'
                case TopicTypes.READ_SINGLE_VAL:
                    return 'myhab/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/value'
                case TopicTypes.WRITE_SINGLE_VAL:
                    return 'myhab/$map.deviceCode/$map.portType/$map.portCode/value'
                case TopicTypes.STAT_IP:
                    return 'myhab/$map.deviceCode/sensor/esp_ip_address/state'
                case TopicTypes.STAT_PORT:
                    return 'myhab/$map.deviceCode/sensor/esp_ip_address/state'
                case TopicTypes.STATUS:
                    return 'myhab/(\\w+|_+)/status'
                case TopicTypes.STATUS_WRITE:
                    return 'myhab/$map.deviceCode/status'
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }
}