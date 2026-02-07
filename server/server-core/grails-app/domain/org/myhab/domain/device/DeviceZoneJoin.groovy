package org.myhab.domain.device


import org.myhab.domain.infra.Zone
import grails.gorm.DetachedCriteria

class DeviceZoneJoin implements Serializable {

    Device device
    Zone zone

    static mapping = {
        id composite: ['device', 'zone']
        table 'zones_devices_join'
        version false
    }

    public static DeviceZoneJoin get(long deviceId, long zoneId) {
        criteriaFor(deviceId, zoneId).get()
    }

    private static DetachedCriteria criteriaFor(long deviceId, long zoneId) {
        DeviceZoneJoin.where {
            device == Device.load(deviceId) && zone == Zone.load(zoneId)
        }
    }
}

