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
        type nullable: true, cascade: "all"
        status nullable: true
        zones joinTable: [name: "zones_devices_join", key: 'device_id'], cascade: "all"
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

