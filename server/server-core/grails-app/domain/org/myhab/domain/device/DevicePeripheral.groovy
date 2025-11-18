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
import org.myhab.domain.device.CablePeripheralJoin
import groovy.util.logging.Slf4j

@Slf4j
class DevicePeripheral extends BaseEntity implements Configurable<DevicePeripheral> {
    String name
    String model
    String description
    double maxAmp
    PeripheralCategory category
    Set<DevicePort> connectedTo
    Set<Zone> zones
    Set<Cable> cables
    Set<PeripheralAccessToken> accessTokens

    static belongsTo = [DevicePort, Zone, PeripheralCategory]
    static hasMany = [connectedTo: DevicePort, zones: Zone, cables: Cable, accessTokens: PeripheralAccessToken]
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
        cables joinTable: [name: "cables_peripherals_join", key: 'peripheral_id'], cascade: "all"
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
                        peripheral.cables?.each { cable ->
                            if (dbExistingPeripheral.cables.find { c -> c.id == cable.id } == null) {
                                dbExistingPeripheral.addToCables(Cable.get(cable.id))
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

                        // Process cables - handle both Map and object access
                        def cablesInput = peripheral.cables ?: peripheral.get('cables')
                        if (cablesInput != null) {
                            // Force load the cables collection by accessing it
                            def existingCables = dbExistingPeripheral.cables ?: []
                            
                            // Create a list of cable IDs from input for easier comparison
                            def inputCableIds = cablesInput.collect { c -> 
                                def id = c.id ?: c.get('id')
                                id != null ? Long.valueOf(id) : null
                            }.findAll { it != null }
                            
                            // Remove cables that are not in the input - use explicit join entity
                            existingCables.each { dbCable ->
                                def cableExists = cablesInput.find { c -> 
                                    def cableId = c.id ?: c.get('id')
                                    def matches = cableId == dbCable.id || (cableId != null && Long.valueOf(cableId) == dbCable.id)
                                    matches
                                }
                                if (cableExists == null) {
                                    // Use explicit join entity deletion
                                    def join = CablePeripheralJoin.get(dbCable.id, peripheralId)
                                    if (join) {
                                        join.delete(failOnError: true, flush: true)
                                    }
                                }
                            }
                            
                            // Add new cables that are not already in the collection - use explicit join entity
                            def existingCableIds = existingCables*.id
                            inputCableIds.each { cableIdLong ->
                                if (!existingCableIds.contains(cableIdLong)) {
                                    def cableEntity = Cable.get(cableIdLong)
                                    if (cableEntity) {
                                        // Check if join already exists
                                        def existingJoin = CablePeripheralJoin.get(cableIdLong, peripheralId)
                                        if (existingJoin == null) {
                                            // Create explicit join entity
                                            new CablePeripheralJoin(
                                                cable: cableEntity,
                                                peripheral: dbExistingPeripheral
                                            ).save(failOnError: true, flush: true)
                                        }
                                    } else {
                                        log.warn("updatePeripheral: Cable with id ${cableIdLong} not found in database")
                                    }
                                }
                            }
                        }
                        
                        // Save the peripheral with all changes
                        dbExistingPeripheral.save(failOnError: true, flush: true)
                        
                        // Refresh to ensure collection is properly loaded
                        dbExistingPeripheral.refresh()
                    }
                    return [success: true, error: null]
                }
            })
        }
    }
}
