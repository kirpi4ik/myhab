package eu.devexpert.madhouse.domain

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.DevicePeripheral

class PeripheralAccessToken extends BaseEntity {
    String token
    DevicePeripheral peripheral
    User user
    Date tsExpiration
    static belongsTo = [user: User, peripheral: DevicePeripheral]

    static mapping = {
        table '`peripheral_access_tokens`'
    }
    static constraints = {
        user nullable: true
    }
}
