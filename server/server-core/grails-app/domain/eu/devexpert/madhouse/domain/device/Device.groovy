package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.port.DevicePort
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class Device extends BaseEntity implements Configurable<Device> {
    String code
    String name
    DeviceModel model
    DeviceType type
    DeviceStatus status
    String description
    String offlineScenario
    NetworkAddress networkAddress
    Set<DevicePort> ports
    Set<DeviceAccount> authAccounts
    Rack rack;
    List<DeviceBackup> backups


    static belongsTo = [rack: Rack, type: DeviceType]
    static hasMany = [authAccounts: DeviceAccount, ports: DevicePort, backups: DeviceBackup]

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
        query('deviceByUid', Device) {
            argument('uid', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Device.findByUid(environment.getArgument('uid'))
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

