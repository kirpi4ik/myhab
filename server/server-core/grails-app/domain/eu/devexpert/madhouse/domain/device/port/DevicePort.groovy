package eu.devexpert.madhouse.domain.device.port

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.Scenario
import eu.devexpert.madhouse.domain.job.EventSubscription

class DevicePort extends BaseEntity implements Configurable<DevicePort> {
    String internalRef
    String name
    String description
    Device device
    PortType type = PortType.UNKNOW
    PortState state = PortState.UNKNOW
    Set<DevicePeripheral> peripherals
    Set<Cable> cables
    Set<Scenario> scenarios
    Set<EventSubscription> subscriptions
    boolean mustSendToServer
    String mode
    String model
    boolean runScenario
    String action
    boolean runAction
    String value
    String miscValue
    String hystDeviationValue

    static hasMany = [cables: Cable, peripherals: DevicePeripheral, scenarios: Scenario, subscriptions: EventSubscription]

    static belongsTo = Device

    static constraints = {
        internalRef nullable: true
        name nullable: true
        type nullable: true
        state nullable: true
        description nullable: true
        mode nullable: true
        model nullable: true
        value nullable: true
        hystDeviationValue nullable: true
        action nullable: true
        miscValue nullable: true
        mustSendToServer nullable: true
    }
    static mapping = {
        table '`device_ports`'
        cables joinTable: [name: "device_ports_cables_join", key: 'port_id']
        peripherals joinTable: [name: "device_ports_peripherals_join", key: 'port_id']
        scenarios joinTable: [name: "device_ports_scenarios_join", key: 'port_id']
        sort name: "asc"
        peripherals cascade: 'save-update'
    }
    static graphql = org.grails.gorm.graphql.entity.dsl.GraphQLMapping.lazy {
        add('configurations', [Configuration]) {
            dataFetcher { BaseEntity entity ->
                Configuration.where {
                    entityId == entity.id && entityType == EntityType.get(entity)
                }.order("name", "desc")
            }
            input false
        }
    }
}

