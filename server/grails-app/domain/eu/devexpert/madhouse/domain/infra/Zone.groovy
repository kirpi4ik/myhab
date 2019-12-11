package eu.devexpert.madhouse.domain.infra

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.SingleEntityDataFetcher

@Resource(readOnly = true, formats = ['json', 'xml'])
class Zone extends BaseEntity {
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
        peripherals joinTable: [name: "zones_peripherals_join", key: 'zone_id']
        cables joinTable: [name: "zones_cables_join", key: 'zone_id']
        sort parent: "asc"
    }
    static constraints = {
        peripherals cascade: 'save-update'
        cables cascade: 'save-update'
        zones cascade: 'save-update'
    }
    static graphql = GraphQLMapping.lazy {
        query('zoneByUid', Zone) {
            argument('uid', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Zone.findByUid(environment.getArgument('uid'))
                }
            })
        }
        query('zonesByParentUid', [Zone]) {
            argument('uid', String)
            dataFetcher(new EntityDataFetcher<Zone>(Zone.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Zone.where {
                        parent {
                            eq('uid', environment.getArgument('uid') as String)
                        }
                    }
                }
            })
        }
        query('zonesRoot', [Zone]) {
            dataFetcher(new EntityDataFetcher<Zone>(Zone.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Zone.where { parent == null }

                }
            })
        }
    }

}