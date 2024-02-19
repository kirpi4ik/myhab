package org.myhab.async.mqtt

enum MQTTTopic {
    public static final String MYHAB_PREFIX = "myhab"
    class COMMON implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return "$MYHAB_PREFIX/#"
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
                    return "$MYHAB_PREFIX/#"
                case TopicTypes.READ_SINGLE_VAL:
                    return "$MYHAB_PREFIX/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/state"
                case TopicTypes.WRITE_SINGLE_VAL:
                    return "$MYHAB_PREFIX/\$map.deviceCode/\$map.portType/\$map.portCode/cmd"
                case TopicTypes.STAT_IP:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/esp_ip_address/state"
                case TopicTypes.STAT_PORT:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/esp_ip_address/state"
                case TopicTypes.STATUS:
                    return "$MYHAB_PREFIX/(\\w+|_+)/status"
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
                    return "$MYHAB_PREFIX/#"
                case TopicTypes.READ_SINGLE_VAL:
                    return "$MYHAB_PREFIX/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/value"
                case TopicTypes.WRITE_SINGLE_VAL:
                    return "$MYHAB_PREFIX/\$map.deviceCode/\$map.portType/\$map.portCode/value"
                case TopicTypes.STAT_IP:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/ip_address/value"
                case TopicTypes.STAT_PORT:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/ip_address/value"
                case TopicTypes.STATUS:
                    return "$MYHAB_PREFIX/(\\w+|_+)/status"
                case TopicTypes.STATUS_WRITE:
                    return "$MYHAB_PREFIX/\$map.deviceCode/status"
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }
    class ELECTRIC_METER_DTS implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return "$MYHAB_PREFIX/#"
                case TopicTypes.READ_SINGLE_VAL:
                    return "$MYHAB_PREFIX/(\\w+|_+)/emeters/(\\w+|_+)/(\\w+|_+)/value"
                case TopicTypes.WRITE_SINGLE_VAL:
                    return "$MYHAB_PREFIX/\$map.deviceCode/\$map.portType/\$map.portCode/value"
                case TopicTypes.STAT_IP:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/esp_ip_address/value"
                case TopicTypes.STAT_PORT:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/esp_ip_address/value"
                case TopicTypes.STATUS:
                    return "$MYHAB_PREFIX/(\\w+|_+)/(\\w+|_+)/status"
                case TopicTypes.STATUS_WRITE:
                    return "$MYHAB_PREFIX/\$map.deviceCode/status"
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }
    class INVERTER implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return "$MYHAB_PREFIX/#"
                case TopicTypes.READ_SINGLE_VAL:
                    return "$MYHAB_PREFIX/(\\w+|_+)/inverter/(\\w+|_+)/(\\w+|_+)/value"
                case TopicTypes.WRITE_SINGLE_VAL:
                    return "$MYHAB_PREFIX/\$map.deviceCode/\$map.portType/\$map.portCode/value"
                case TopicTypes.STAT_IP:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/ip_address/value"
                case TopicTypes.STAT_PORT:
                    return "$MYHAB_PREFIX/\$map.deviceCode/sensor/ip_address/value"
                case TopicTypes.STATUS:
                    return "$MYHAB_PREFIX/(\\w+|_+)/(\\w+|_+)/status"
                case TopicTypes.STATUS_WRITE:
                    return "$MYHAB_PREFIX/\$map.deviceCode/status"
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }

    class ONVIF implements DeviceTopic {
        static String topic(TopicTypes topicType) {
            switch (topicType) {
                case TopicTypes.LISTEN:
                    return 'onvif2mqtt/#'
                case TopicTypes.READ_SINGLE_VAL:
                    return 'onvif2mqtt/(\\w+|_+)/motion'
                default: return null
            }
        }

        @Override
        String topicByType(TopicTypes topicTypes) {
            return topic(topicTypes)
        }
    }
}