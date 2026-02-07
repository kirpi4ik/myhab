package org.myhab.domain.device


import org.myhab.domain.common.BaseEntity
import org.myhab.domain.common.Configurable
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.infra.Zone

class Device extends BaseEntity implements Configurable<Device> {
    String code
    String name
    DeviceModel model
    DeviceCategory type
    DeviceStatus status
    String description
    String offlineScenario
    NetworkAddress networkAddress
    Set<DevicePort> ports
    Set<DeviceAccount> authAccounts
    Rack rack;
    List<DeviceBackup> backups
    Set<Zone> zones

    static belongsTo = [rack: Rack, type: DeviceCategory]
    static hasMany = [authAccounts: DeviceAccount, zones: Zone, ports: DevicePort, backups: DeviceBackup]

    static mapping = {
        table '`device_controllers`'
        sort name: "asc"
        authAccounts sort: 'username', order: 'asc'
        ports sort: 'name', order: 'asc'
    }
    static constraints = {
        code nullable: false, unique: true
        name nullable: true
        model nullable: true
        description nullable: true
        networkAddress nullable: true, cascade: "all"
        offlineScenario nullable: true
        rack nullable: true
        type nullable: true
        status nullable: true
        zones joinTable: [name: "zones_devices_join", key: 'device_id']
    }
    static embedded = ['networkAddress']
    static graphql = GraphQLMapping.lazy {
        add('configurations', [Configuration]) {
            dataFetcher { BaseEntity entity ->
                Configuration.where {
                    entityId == entity.id && entityType == EntityType.get(entity)
                }.order("name", "desc")
            }
            input false
        }
        
        // Custom update mutation to properly handle zones many-to-many relationship
        mutation('deviceUpdateCustom', Device) {
            argument('id', Long)
            argument('device', Device.class)
            returns Device
            dataFetcher { DataFetchingEnvironment env ->
                Long id = env.getArgument('id') as Long
                Device deviceData = env.getArgument('device') as Device
                
                Device existingDevice = Device.get(id)
                if (!existingDevice) {
                    throw new RuntimeException("Device not found with id: ${id}")
                }
                
                Device.withTransaction {
                    // Update basic fields
                    if (deviceData.code != null) existingDevice.code = deviceData.code
                    if (deviceData.name != null) existingDevice.name = deviceData.name
                    if (deviceData.description != null) existingDevice.description = deviceData.description
                    if (deviceData.offlineScenario != null) existingDevice.offlineScenario = deviceData.offlineScenario
                    if (deviceData.networkAddress != null) existingDevice.networkAddress = deviceData.networkAddress
                    
                    // Update model - handle enum
                    if (deviceData.model != null) {
                        existingDevice.model = deviceData.model
                    }
                    
                    // Update status - handle enum
                    if (deviceData.status != null) {
                        existingDevice.status = deviceData.status
                    }
                    
                    // Update rack - get managed entity or clear
                    if (deviceData.rack != null) {
                        def newRackId = deviceData.rack?.id as Long
                        if (newRackId) {
                            existingDevice.rack = Rack.get(newRackId)
                        } else {
                            existingDevice.rack = null
                        }
                    }
                    
                    // Update type - get managed entity or clear
                    if (deviceData.type != null) {
                        def newTypeId = deviceData.type?.id as Long
                        if (newTypeId) {
                            existingDevice.type = DeviceCategory.get(newTypeId)
                        } else {
                            existingDevice.type = null
                        }
                    }
                    
                    // Save basic changes first
                    existingDevice.save(flush: true, failOnError: true)
                    
                    // Update zones - rebuild the collection using explicit join table
                    if (deviceData.zones != null) {
                        def newZoneIds = deviceData.zones?.collect { it?.id as Long }?.findAll { it != null } ?: []
                        
                        // Clear all existing zone relationships for this device
                        Device.executeUpdate(
                            'delete from DeviceZoneJoin dzj where dzj.device.id = :deviceId',
                            [deviceId: existingDevice.id]
                        )
                        
                        // Add new zone relationships
                        newZoneIds.each { zoneId ->
                            Zone zone = Zone.get(zoneId)
                            if (zone) {
                                new DeviceZoneJoin(device: existingDevice, zone: zone).save(flush: true, failOnError: true)
                            }
                        }
                    }
                }
                
                // Refresh to get latest state
                existingDevice.refresh()
                
                return existingDevice
            }
        }
        
        query('deviceById', Device) {
            argument('id', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Device.findById(environment.getArgument('id'))
                }
            })
        }
    }
}

class NetworkAddress {
    String ip
    String gateway
    String port
    static constraints = {
        gateway nullable: true
    }
}

