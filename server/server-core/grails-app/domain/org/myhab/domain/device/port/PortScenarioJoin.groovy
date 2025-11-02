package org.myhab.domain.device.port

import org.myhab.domain.device.Scenario
import grails.gorm.DetachedCriteria

class PortScenarioJoin implements Serializable {

    DevicePort port
    Scenario scenario

    static mapping = {
        id composite: ['port', 'scenario']
        table 'device_ports_scenarios_join'
        version false
    }

    public static PortScenarioJoin get(long portId, long scenarioId) {
        criteriaFor(portId, scenarioId).get()
    }

    private static DetachedCriteria criteriaFor(long portId, long scenarioId) {
        PortScenarioJoin.where {
            port == DevicePort.load(portId) && scenario == Scenario.load(scenarioId)
        }
    }
}

