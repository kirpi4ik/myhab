package org.myhab.domain.device

import org.myhab.domain.device.DevicePeripheral
import grails.gorm.DetachedCriteria

class CablePeripheralJoin implements Serializable {

    Cable cable
    DevicePeripheral peripheral

    static mapping = {
        id composite: ['cable', 'peripheral']
        table 'cables_peripherals_join'
        version false
    }

    public static CablePeripheralJoin get(long cableId, long peripheralId) {
        criteriaFor(cableId, peripheralId).get()
    }

    private static DetachedCriteria criteriaFor(long cableId, long peripheralId) {
        CablePeripheralJoin.where {
            cable == Cable.load(cableId) && peripheral == DevicePeripheral.load(peripheralId)
        }
    }
}

