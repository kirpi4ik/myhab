package org.myhab.domain.device

enum DeviceStatus {
    ONLINE,
    OFFLINE,
    DISABLED

    static def DeviceStatus fromValue(def name) {
        values().find { val->
            val.name().equalsIgnoreCase("$name")
        }
    }
}