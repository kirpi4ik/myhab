package eu.devexpert.madhouse.async.mqtt

enum MQTTTopic {
    enum COMMON {
        DEFAULT('madhouse/#')
        def regex

        COMMON(regex) {
            this.regex = regex
        }
    }

    enum MEGA implements DeviceTopic {
        LISTEN('$map.deviceCode/#'),
        READ_SINGLE_VAL('(\\w+)/([0-9]+)'),
        WRITE_SINGLE_VAL('$map.deviceCode/cmd'),
        STAT_IP('$map.deviceCode/address_ip/state'),
        STAT_PORT('$map.deviceCode/address_port/state'),
        STATUS('$map.deviceCode/status') //madhouse/<deviceCode>/<portType>/<portRef>/state

        def regex

        MEGA(regex) {
            this.regex = regex
        }
    }

    enum ESP implements DeviceTopic {
        LISTEN('madhouse/#'),
        READ_SINGLE_VAL('madhouse/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/state'), //madhouse/<deviceCode>/<portType>/<portRef>/state
        WRITE_SINGLE_VAL('madhouse/$map.deviceCode/$map.portType/$map.portCode/cmd'), //madhouse/<deviceCode>/<portType>/<portRef>/state
        STAT_IP('madhouse/$map.deviceCode/sensor/esp_ip_address/state'),
        STAT_PORT('madhouse/$map.deviceCode/sensor/esp_ip_address/state'),
        STATUS('madhouse/(\\w+|_+)/status') //madhouse/<deviceCode>/<portType>/<portRef>/state //online | offline

        def regex

        ESP(regex) {
            this.regex = regex
        }
    }
    enum NIBE implements DeviceTopic {
        LISTEN('madhouse/#'),
        READ_SINGLE_VAL('madhouse/(\\w+|_+)/(\\w+|_+)/(\\w+|_+)/value'), //madhouse/<deviceCode>/<portType>/<portRef>/state
        WRITE_SINGLE_VAL('madhouse/$map.deviceCode/$map.portType/$map.portCode/value'), //madhouse/<deviceCode>/<portType>/<portRef>/state
        STAT_IP('madhouse/$map.deviceCode/sensor/esp_ip_address/state'),
        STAT_PORT('madhouse/$map.deviceCode/sensor/esp_ip_address/state'),
        STATUS('madhouse/(\\w+|_+)/status') //madhouse/<deviceCode>/<portType>/<portRef>/state //online | offline

        def regex

        NIBE(regex) {
            this.regex = regex
        }
    }


}