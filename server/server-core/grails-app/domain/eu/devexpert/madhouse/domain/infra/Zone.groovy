package eu.devexpert.madhouse.domain.infra

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Configurable
import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher

@Resource(readOnly = true, formats = ['json', 'xml'])
class Zone extends BaseEntity implements Configurable<Zone> {
    String name
    String description
    Set<String> categories
    Zone parent
    Set<Zone> zones
    Set<DevicePeripheral> peripherals
    Set<Cable> cables

    static belongsTo = [parent: Zone]
    static hasOne = [parent: Zone]
    static hasMany = [cables: Cable, peripherals: DevicePeripheral, zones: Zone]


    static mapping = {
        table '`zones`'
        sort name: "asc"
        zones sort: "name"
        peripherals joinTable: [name: "zones_peripherals_join", key: 'zone_id']
        cables joinTable: [name: "zones_cables_join", key: 'zone_id']

    }
    static constraints = {
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
        query('zonesRoot', [Zone]) {
            dataFetcher(new EntityDataFetcher<Zone>(Zone.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Zone.where { parent == null }.order("name", "desc")
                }
            })
        }
    }

}