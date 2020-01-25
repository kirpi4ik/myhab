package eu.devexpert.madhouse.domain

import grails.gorm.DetachedCriteria
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher
import org.grails.gorm.graphql.fetcher.impl.EntityDataFetcher

class Configuration {
    Long id
    String key
    Long entityId
    EntityType entityType

    String name
    String value
    String description

    static mapping = {
        table '`configurations`'
    }
    static constraints = {
        name nullable: true
        description nullable: true
    }
    static graphql = GraphQLMapping.lazy {
        query('configPropertyByKey', Configuration) {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Configuration.where {
                        entityId == environment.getArgument('entityId') && entityType == environment.getArgument('entityType') && key == environment.getArgument('key')
                    }.first()
                }
            })
        }
        query('configListByKey', [Configuration]) {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            dataFetcher(new EntityDataFetcher<Configuration>(Configuration.gormPersistentEntity) {
                @Override
                protected DetachedCriteria buildCriteria(DataFetchingEnvironment environment) {
                    Configuration.where {
                        entityId == environment.getArgument('entityId') && entityType == environment.getArgument('entityType') && key == environment.getArgument('key')
                    }.order("name", "desc")
                }
            })
        }
        query('configPropertyByKey', Configuration) {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Configuration.where {
                        entityId == environment.getArgument('entityId') && entityType == environment.getArgument('entityType') && key == environment.getArgument('key')
                    }.order("name", "desc")[0]
                }
            })
        }
        query('configKeysByEntity', [String]) {
            argument('entityType', EntityType)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    Configuration.withCriteria {
                        eq('entityType', environment.getArgument('entityType') as EntityType)
                        projections {
                            distinct("key")
                        }
                    }
                }
            })
        }
        mutation('configDeleteByKey', "ConfigurationDeleteResult") {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            returns {
                field('error', String)
                field('success', Boolean)
            }
            dataFetcher(new DeleteEntityDataFetcher(Configuration.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    withTransaction(false) {
                        Configuration.where {
                            entityId == environment.getArgument('entityId') && entityType == environment.getArgument('entityType') && key == environment.getArgument('key')
                        }.deleteAll()
                    }
                    return [success: true, error: null]
                }
            })
        }
    }
}
