package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.PeripheralAccessToken
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.infra.Zone
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher

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
    Set<PeripheralAccessToken> accessTokens

    static belongsTo = [DevicePort, Zone, PeripheralCategory]
    static hasMany = [connectedTo: DevicePort, zones: Zone, accessTokens : PeripheralAccessToken]
    static hasOne = [category: PeripheralCategory]

    static constraints = {
        name nullable: false
        code nullable: false
        model nullable: true
        description nullable: true
        maxAmp nullable: true
        codeOld nullable: true
    }
    static mapping = {
        table '`device_peripherals`'
        sort name: "asc"
        connectedTo joinTable: [name: "device_ports_peripherals_join", key: 'peripheral_id'], cascade: "all"
        zones joinTable: [name: "zones_peripherals_join", key: 'peripheral_id'], cascade: "all"
        accessTokens joinTable: [name: "peripherals_access_tokens_join", key: 'peripheral_id'], cascade: "all"
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
        mutation('updatePeripheral', "DevicePeripheralUpdateResult") {
            argument('id', Long)
            argument('peripheral', DevicePeripheral.class)
            returns {
                field('error', String)
                field('success', Boolean)
            }
            dataFetcher(new UpdateEntityDataFetcher(DevicePeripheral.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    def peripheralId = environment.getArgument("id")
                    def peripheral = environment.getArgument("peripheral")
                    withTransaction(false) {
                        DevicePeripheral dbExistingPeripheral = DevicePeripheral.findById(peripheralId)
                        peripheral.entrySet().findAll { entry -> !(entry.value instanceof Collection) }.each { entry ->
                            if (entry.key == 'category') {
                                dbExistingPeripheral.category = PeripheralCategory.get(entry.value.id)
                            } else {
                                if (DevicePeripheral.metaClass.hasProperty(dbExistingPeripheral, entry.key) && entry.key != 'metaClass' && entry.key != 'class') {
                                    dbExistingPeripheral.setProperty(entry.key, entry.value)
                                }
                            }
                        }


                        def existingZones = []
                        existingZones.addAll dbExistingPeripheral.zones
                        existingZones.each { dbZone ->
                            if (peripheral.zones.find { z -> z.id == dbZone.id } == null) {
                                dbExistingPeripheral.removeFromZones(dbZone).save(failOnError: true, flush: true)
                                dbZone.removeFromPeripherals(dbExistingPeripheral).save(failOnError: true, flush: true)
                            }
                        }
                        peripheral.zones.each { mapZone ->
                            if (dbExistingPeripheral.zones.find { z -> z.id == mapZone.id } == null) {
                                dbExistingPeripheral.addToZones(Zone.get(mapZone.id))
                            }
                        }

                        def existingPorts = []
                        existingPorts.addAll dbExistingPeripheral.connectedTo
                        existingPorts.each { port ->
                            if (peripheral.connectedTo.find { p -> p.id == port.id } == null) {
                                dbExistingPeripheral.removeFromConnectedTo(port).save(failOnError: true, flush: true)
                                port.removeFromPeripherals(dbExistingPeripheral).save(failOnError: true, flush: true)
                            }
                        }
                        peripheral.connectedTo.each { port ->
                            if (dbExistingPeripheral.connectedTo.find { p -> p.id == port.id } == null) {
                                dbExistingPeripheral.addToConnectedTo(DevicePort.get(port.id))
                            }
                        }
                        dbExistingPeripheral.save(failOnError: true, flush: true)
                    }
                    return [success: true, error: null]
                }
            })
        }
    }
}
