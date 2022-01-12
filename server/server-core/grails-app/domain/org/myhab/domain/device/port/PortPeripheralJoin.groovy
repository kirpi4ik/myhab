package org.myhab.domain.device.port


import org.myhab.domain.device.DevicePeripheral
import grails.gorm.DetachedCriteria

class PortPeripheralJoin implements Serializable {

    DevicePort port
    DevicePeripheral peripheral

    static mapping = {
        id composite: ['port', 'peripheral']
        table 'device_ports_peripherals_join'
        version false
    }

    public static PortPeripheralJoin get(long portId, long peripheralId) {
        criteriaFor(portId, peripheralId).get()
    }

    private static DetachedCriteria criteriaFor(long portId, long peripheralId) {
        PortPeripheralJoin.where {
            port == DevicePort.load(portId) && peripheral == DevicePeripheral.load(peripheralId)
        }
    }
}
