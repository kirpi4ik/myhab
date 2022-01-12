package org.myhab.domain.device


import org.myhab.domain.infra.Zone
import grails.gorm.DetachedCriteria

class CableZoneJoin implements Serializable {

    Cable cable
    Zone zone

    static mapping = {
        id composite: ['cable', 'zone']
        table 'zones_cables_join'
        version false
    }

    public static CableZoneJoin get(long cableId, long zoneId) {
        criteriaFor(cableId, zoneId).get()
    }

    private static DetachedCriteria criteriaFor(long cableId, long zoneId) {
        CableZoneJoin.where {
            cable == Cable.load(cableId) && zone == Zone.load(zoneId)
        }
    }
}
