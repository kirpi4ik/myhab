package org.myhab.domain.device

import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.infra.Zone
import grails.gorm.DetachedCriteria

class ZonePeripheralJoin implements Serializable {

    Zone zone
    DevicePeripheral peripheral

    static mapping = {
        id composite: ['zone', 'peripheral']
        table 'zones_peripherals_join'
        version false
    }

    public static ZonePeripheralJoin get(long zoneId, long peripheralId) {
        criteriaFor(zoneId, peripheralId).get()
    }

    private static DetachedCriteria criteriaFor(long zoneId, long peripheralId) {
        ZonePeripheralJoin.where {
            zone == Zone.load(zoneId) && peripheral == DevicePeripheral.load(peripheralId)
        }
    }
}

