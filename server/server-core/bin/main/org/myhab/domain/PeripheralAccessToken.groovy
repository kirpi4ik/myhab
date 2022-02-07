package org.myhab.domain

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.DevicePeripheral

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
