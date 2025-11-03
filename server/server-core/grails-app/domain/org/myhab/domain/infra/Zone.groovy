package org.myhab.domain.infra

import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.common.BaseEntity
import org.myhab.domain.common.Configurable
import org.myhab.domain.device.Cable
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral

@Resource(readOnly = true, formats = ['json', 'xml'])
class Zone extends BaseEntity implements Configurable<Zone> {
    String name
    String description
    Set<String> categories
    Zone parent
    Set<Zone> zones
    Set<Device> devices
    Set<DevicePeripheral> peripherals
    Set<Cable> cables

    static belongsTo = [parent: Zone]
    static hasOne = [parent: Zone]
    static hasMany = [cables: Cable, peripherals: DevicePeripheral, devices: Device, zones: Zone]


    static mapping = {
        table '`zones`'
        sort name: "asc"
        zones sort: "name"
        devices joinTable: [name: "zones_devices_join", key: 'zone_id']
        peripherals joinTable: [name: "zones_peripherals_join", key: 'zone_id']
        cables joinTable: [name: "zones_cables_join", key: 'zone_id']

    }
    static constraints = {
        devices cascade: 'save-update'
        peripherals cascade: 'save-update'
        cables cascade: 'save-update'
        zones cascade: 'save-update'
    }

    static graphql = GraphQLMapping.lazy {
        add('configurations', [Configuration]) {
            dataFetcher { BaseEntity entity ->
                Configuration.where {
                    entityId == entity.id && entityType == EntityType.get(entity)
                }.order("name", "asc")
            }
            input true
        }

        // Custom update mutation to properly handle sub-zones removal
        mutation('zoneUpdateCustom', Zone) {
            argument('id', Long)
            argument('zone', Zone.class)
            returns Zone
            dataFetcher { DataFetchingEnvironment env ->
                Long id = env.getArgument('id') as Long
                Zone zoneData = env.getArgument('zone') as Zone
                
                Zone existingZone = Zone.get(id)
                if (!existingZone) {
                    throw new RuntimeException("Zone not found with id: ${id}")
                }
                
                Zone.withTransaction { status ->
                    // Update basic fields
                    if (zoneData.name != null) existingZone.name = zoneData.name
                    if (zoneData.description != null) existingZone.description = zoneData.description
                    if (zoneData.categories != null) existingZone.categories = zoneData.categories
                    
                    // Update parent - get managed entity or clear
                    def newParentId = zoneData.parent?.id as Long
                    if (newParentId != existingZone.parent?.id) {
                        existingZone.parent = newParentId ? Zone.load(newParentId) : null
                    }
                    
                    // Update sub-zones - rebuild the collection
                    def oldZoneIds = existingZone.zones?.collect { it.id } ?: []
                    def newZoneIds = zoneData.zones?.collect { it.id } ?: []
                    
                    // Find zones that need to be removed (in old but not in new)
                    def zonesToRemove = oldZoneIds - newZoneIds
                    
                    // Clear parent on removed zones BEFORE modifying the collection
                    zonesToRemove.each { zoneId ->
                        Zone.executeUpdate("UPDATE Zone z SET z.parent = NULL WHERE z.id = :zoneId", [zoneId: zoneId])
                    }
                    
                    // Now rebuild the zones collection
                    existingZone.zones?.clear()
                    
                    // Add all zones from the new list
                    newZoneIds.each { zoneId ->
                        Zone zone = Zone.load(zoneId as Long)
                        if (zone) {
                            existingZone.addToZones(zone)
                        }
                    }
                    
                    existingZone.save(flush: true, failOnError: true)
                }
                
                // Refresh to get latest state
                existingZone.refresh()
                
                return existingZone
            }
        }

        query('zoneById', Zone) {
            argument('id', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Zone.findById(environment.getArgument('id') as Long, [sort: "name", order: "asc"])
                }
            })
        }
        query('zonesByParentId', [Zone]) {
            argument('id', String)
            dataFetcher(new EntityDataFetcher<Zone>(Zone.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Zone.where {
                        parent {
                            eq('id', environment.getArgument('id') as Long)
                        }
                    }.order("name", "asc")

                }
            })
        }
        query('zonesRoot', [Zone]) {
            dataFetcher(new EntityDataFetcher<Zone>(Zone.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Zone.where { parent == null }.order("name", "asc")
                }
            })
        }
    }

}