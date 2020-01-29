package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.infra.Zone
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class DevicePeripheral extends BaseEntity implements Configurable<DevicePeripheral> {
    String code
    String codeOld
    String name
    String model
    String description
    double maxAmp
    PeripheralCategory category
    Set<DevicePort> connectedTo
    Set<Zone> zones

    static belongsTo = [DevicePort, Zone, PeripheralCategory]
    static hasMany = [connectedTo: DevicePort, zones: Zone]
    static hasOne = [category: PeripheralCategory]

    static constraints = {
        name nullable: true
        model nullable: true
        description nullable: true
    }
    static mapping = {
        table '`device_peripherals`'
        connectedTo joinTable: [name: "device_ports_peripherals_join", key: 'peripheral_id']
        zones joinTable: [name: "zones_peripherals_join", key: 'peripheral_id']
        sort name: "asc"
    }

    static graphql = GraphQLMapping.lazy {
        add('configurations', [Configuration]) {
            dataFetcher { BaseEntity entity ->
                Configuration.where {
                    entityId == entity.id && entityType == EntityType.get(entity)
                }.order("name", "desc")
            }
            input false
        }
        query('devicePeripheralByUid', DevicePeripheral) {
            argument('uid', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    DevicePeripheral.findByUid(environment.getArgument('uid'))
                }
            })
        }
    }
}
