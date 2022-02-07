package org.myhab.domain.device


import org.myhab.domain.device.port.DevicePort
import grails.gorm.DetachedCriteria

class CablePortJoin  implements Serializable {

    Cable cable
    DevicePort port

    static mapping = {
        id composite: ['cable', 'port']
        table 'device_ports_cables_join'
        version false
    }
    public static CablePortJoin get(long cableId, long portId) {
        criteriaFor(cableId, portId).get()
    }
    private static DetachedCriteria criteriaFor(long cableId, long portId) {
        CablePortJoin.where {
            cable == Cable.load(cableId) && port == DevicePort.load(portId)
        }
    }
}
