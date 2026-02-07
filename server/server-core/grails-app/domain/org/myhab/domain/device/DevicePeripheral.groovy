package org.myhab.domain.device

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.CreateEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.UpdateEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.PeripheralAccessToken
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.common.Configurable
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortPeripheralJoin
import org.myhab.domain.infra.Zone
import org.myhab.domain.device.CablePeripheralJoin
import org.myhab.domain.device.ZonePeripheralJoin
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

    static belongsTo = [category: PeripheralCategory]
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
        connectedTo joinTable: [name: "device_ports_peripherals_join", key: 'peripheral_id'], cascade: "save-update"
        zones joinTable: [name: "zones_peripherals_join", key: 'peripheral_id'], cascade: "save-update"
        cables joinTable: [name: "cables_peripherals_join", key: 'peripheral_id'], cascade: "save-update"
        accessTokens joinTable: [name: "peripherals_access_tokens_join", key: 'peripheral_id'], cascade: "save-update"
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
                    DevicePeripheral.withTransaction {
                        peripheral.entrySet().findAll { entry -> !(entry.value instanceof Collection) }.each { entry ->
                            if (entry.key == 'category') {
                                dbExistingPeripheral.category = PeripheralCategory.get(entry.value.id)
                            } else {
                                if (DevicePeripheral.metaClass.hasProperty(dbExistingPeripheral, entry.key) && entry.key != 'metaClass' && entry.key != 'class') {
                                    dbExistingPeripheral.setProperty(entry.key, entry.value)
                                }
                            }
                        }
                        // Save first to get the ID
                        dbExistingPeripheral.save(failOnError: true, flush: true)
                        
                        // Add zones using explicit join entities
                        peripheral.zones?.each { mapZone ->
                            def zoneId = mapZone.id as Long
                            if (zoneId) {
                                def zone = Zone.get(zoneId)
                                if (zone) {
                                    def existingJoin = ZonePeripheralJoin.get(zoneId, dbExistingPeripheral.id)
                                    if (existingJoin == null) {
                                        new ZonePeripheralJoin(zone: zone, peripheral: dbExistingPeripheral).save(failOnError: true, flush: true)
                                    }
                                }
                            }
                        }
                        // Add ports using explicit join entities
                        peripheral.connectedTo?.each { port ->
                            def portId = port.id as Long
                            if (portId) {
                                def portEntity = DevicePort.get(portId)
                                if (portEntity) {
                                    def existingJoin = PortPeripheralJoin.get(portId, dbExistingPeripheral.id)
                                    if (existingJoin == null) {
                                        new PortPeripheralJoin(port: portEntity, peripheral: dbExistingPeripheral).save(failOnError: true, flush: true)
                                    }
                                }
                            }
                        }
                        // Add cables using explicit join entities
                        peripheral.cables?.each { cable ->
                            def cableId = cable.id as Long
                            if (cableId) {
                                def cableEntity = Cable.get(cableId)
                                if (cableEntity) {
                                    def existingJoin = CablePeripheralJoin.get(cableId, dbExistingPeripheral.id)
                                    if (existingJoin == null) {
                                        new CablePeripheralJoin(cable: cableEntity, peripheral: dbExistingPeripheral).save(failOnError: true, flush: true)
                                    }
                                }
                            }
                        }
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
                    DevicePeripheral.withTransaction {
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


                        // Process zones - use explicit join entity
                        def zonesInput = peripheral.zones ?: peripheral.get('zones')
                        if (zonesInput != null) {
                            def existingZones = dbExistingPeripheral.zones ?: []
                            def inputZoneIds = zonesInput.collect { z -> 
                                def id = z.id ?: z.get('id')
                                id != null ? Long.valueOf(id) : null
                            }.findAll { it != null }
                            
                            // Remove zones that are not in the input
                            existingZones.each { dbZone ->
                                def zoneExists = zonesInput.find { z -> 
                                    def zoneId = z.id ?: z.get('id')
                                    zoneId == dbZone.id || (zoneId != null && Long.valueOf(zoneId) == dbZone.id)
                                }
                                if (zoneExists == null) {
                                    def join = ZonePeripheralJoin.get(dbZone.id, peripheralId)
                                    if (join) {
                                        join.delete(failOnError: true, flush: true)
                                    }
                                }
                            }
                            
                            // Add new zones
                            def existingZoneIds = existingZones*.id
                            inputZoneIds.each { zoneIdLong ->
                                if (!existingZoneIds.contains(zoneIdLong)) {
                                    def zoneEntity = Zone.get(zoneIdLong)
                                    if (zoneEntity) {
                                        def existingJoin = ZonePeripheralJoin.get(zoneIdLong, peripheralId)
                                        if (existingJoin == null) {
                                            new ZonePeripheralJoin(zone: zoneEntity, peripheral: dbExistingPeripheral).save(failOnError: true, flush: true)
                                        }
                                    }
                                }
                            }
                        }

                        // Process ports - use explicit join entity
                        def portsInput = peripheral.connectedTo ?: peripheral.get('connectedTo')
                        if (portsInput != null) {
                            def existingPorts = dbExistingPeripheral.connectedTo ?: []
                            def inputPortIds = portsInput.collect { p -> 
                                def id = p.id ?: p.get('id')
                                id != null ? Long.valueOf(id) : null
                            }.findAll { it != null }
                            
                            // Remove ports that are not in the input
                            existingPorts.each { dbPort ->
                                def portExists = portsInput.find { p -> 
                                    def portId = p.id ?: p.get('id')
                                    portId == dbPort.id || (portId != null && Long.valueOf(portId) == dbPort.id)
                                }
                                if (portExists == null) {
                                    def join = PortPeripheralJoin.get(dbPort.id, peripheralId)
                                    if (join) {
                                        join.delete(failOnError: true, flush: true)
                                    }
                                }
                            }
                            
                            // Add new ports
                            def existingPortIds = existingPorts*.id
                            inputPortIds.each { portIdLong ->
                                if (!existingPortIds.contains(portIdLong)) {
                                    def portEntity = DevicePort.get(portIdLong)
                                    if (portEntity) {
                                        def existingJoin = PortPeripheralJoin.get(portIdLong, peripheralId)
                                        if (existingJoin == null) {
                                            new PortPeripheralJoin(port: portEntity, peripheral: dbExistingPeripheral).save(failOnError: true, flush: true)
                                        }
                                    }
                                }
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
        mutation('deleteDevicePeripheral', "DevicePeripheralDeleteResult") {
            argument('id', Long)
            returns {
                field('success', Boolean)
                field('error', String)
            }
            dataFetcher(new DeleteEntityDataFetcher(DevicePeripheral.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    Long id = environment.getArgument('id') as Long
                    if (!id) {
                        log.warn("deleteDevicePeripheral: ID is required but was null or empty")
                        return [success: false, error: "ID is required"]
                    }
                    
                    try {
                        DevicePeripheral.withTransaction {
                            DevicePeripheral peripheral = DevicePeripheral.get(id)
                            if (!peripheral) {
                                log.warn("DevicePeripheral with id ${id} not found")
                                return [success: false, error: "DevicePeripheral with id ${id} not found"]
                            }
                            
                            // Clean up join table entries before deleting the peripheral
                            // This prevents foreign key constraint violations
                            // Store peripheral reference for use in closures
                            def peripheralEntity = peripheral
                            
                            // Clean up port joins
                            def portJoins = PortPeripheralJoin.where {
                                peripheral == peripheralEntity
                            }.list()
                            if (portJoins) {
                                portJoins.each { join ->
                                    join.delete(flush: false)
                                }
                            }
                            
                            // Clean up zone joins
                            def zoneJoins = ZonePeripheralJoin.where {
                                peripheral == peripheralEntity
                            }.list()
                            if (zoneJoins) {
                                zoneJoins.each { join ->
                                    join.delete(flush: false)
                                }
                            }
                            
                            // Clean up cable joins
                            def cableJoins = CablePeripheralJoin.where {
                                peripheral == peripheralEntity
                            }.list()
                            if (cableJoins) {
                                cableJoins.each { join ->
                                    join.delete(flush: false)
                                }
                            }
                            
                            // Clean up Configuration entries
                            Configuration.where {
                                entityId == id && entityType == EntityType.PERIPHERAL
                            }.deleteAll()
                            
                            // Flush all deletes before deleting the peripheral
                            DevicePeripheral.withSession { session ->
                                session.flush()
                            }
                            
                            // Now delete the peripheral itself
                            peripheral.delete(failOnError: true, flush: true)
                        }
                        log.info("DevicePeripheral ${id} deleted successfully")
                        return [success: true, error: null]
                    } catch (Exception e) {
                        log.error("Error deleting DevicePeripheral with id ${id}: ${e.message}", e)
                        return [success: false, error: e.message ?: "Unknown error occurred"]
                    }
                }
            })
        }
    }
}
