package eu.devexpert.madhouse.domain.device

enum DeviceStatus {
    ONLINE,
    OFFLINE

    static def DeviceStatus fromValue(def name) {
        values().find { val->
            val.name().equalsIgnoreCase("$name")
        }
    }
}