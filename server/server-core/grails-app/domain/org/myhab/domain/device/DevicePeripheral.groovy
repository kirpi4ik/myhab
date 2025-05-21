package org.myhab.domain.device

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.CreateEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.PeripheralAccessToken
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.common.Configurable
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone

class DevicePeripheral extends BaseEntity implements Configurable<DevicePeripheral> {
    String name
    String model
    String description
    double maxAmp
    PeripheralCategory category
    Set<DevicePort> connectedTo
    Set<Zone> zones
    Set<PeripheralAccessToken> accessTokens

    static belongsTo = [DevicePort, Zone, PeripheralCategory]
    static hasMany = [connectedTo: DevicePort, zones: Zone, accessTokens: PeripheralAccessToken]
    static hasOne = [category: PeripheralCategory]

    static constraints = {
        name nullable: false
        model nullable: true
        description nullable: true
        maxAmp nullable: true
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
        query('devicePeripheralById', DevicePeripheral) {
            argument('id', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    DevicePeripheral.findById(environment.getArgument('id'))
                }
            })
        }
        mutation('peripheralCreate', DevicePeripheral) {
            argument('devicePeripheral', DevicePeripheral.class)

            returns DevicePeripheral
            dataFetcher(new CreateEntityDataFetcher<DevicePeripheral>(DevicePeripheral.gormPersistentEntity) {
                @Override
                DevicePeripheral get(DataFetchingEnvironment environment) throws Exception {
                    def peripheral = environment.getArgument("devicePeripheral")
                    DevicePeripheral dbExistingPeripheral = new DevicePeripheral();
                    withTransaction(false) {
                        peripheral.entrySet().findAll { entry -> !(entry.value instanceof Collection) }.each { entry ->
                            if (entry.key == 'category') {
                                dbExistingPeripheral.category = PeripheralCategory.get(entry.value.id)
                            } else {
                                if (DevicePeripheral.metaClass.hasProperty(dbExistingPeripheral, entry.key) && entry.key != 'metaClass' && entry.key != 'class') {
                                    dbExistingPeripheral.setProperty(entry.key, entry.value)
                                }
                            }
                        }
                        peripheral.zones.each { mapZone ->
                            if (dbExistingPeripheral.zones.find { z -> z.id == mapZone.id } == null) {
                                dbExistingPeripheral.addToZones(Zone.get(mapZone.id))
                            }
                        }
                        peripheral.connectedTo.each { port ->
                            if (dbExistingPeripheral.connectedTo.find { p -> p.id == port.id } == null) {
                                dbExistingPeripheral.addToConnectedTo(DevicePort.get(port.id))
                            }
                        }
                        dbExistingPeripheral.save(failOnError: true, flush: true)
                    }
                    return dbExistingPeripheral
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
