package eu.devexpert.madhouse.domain

import grails.gorm.DetachedCriteria
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.CreateEntityDataFetcher
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
                    }[0]
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
                    }.order("value", "asc")
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
        mutation('removeConfig', "ConfigurationDeleteByIdResult") {
            argument('id', Long)
            returns {
                field('error', String)
                field('success', Boolean)
            }
            dataFetcher(new DeleteEntityDataFetcher(Configuration.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    withTransaction(false) {
                        Configuration.findById(Long.valueOf(environment.getArgument('id'))).delete()
                    }
                    return [success: true, error: null]
                }
            })
        }
        mutation('configDeleteByKey', "ConfigurationDeleteByKeyResult") {
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
        mutation('savePropertyValue', Configuration) {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            argument('value', String)
            returns Configuration
            dataFetcher(new CreateEntityDataFetcher(Configuration.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    withTransaction(false) {
                        Configuration existingConfig = Configuration.where {
                            entityId == environment.getArgument('entityId') && entityType == environment.getArgument('entityType') && key == environment.getArgument('key')
                        }.order("name", "desc")[0]
                        if (existingConfig == null) {
                            existingConfig = new Configuration()
                            existingConfig.setKey(environment.getArgument('key') as String)
                            existingConfig.setEntityId(environment.getArgument('entityId') as Long)
                            existingConfig.setEntityType(environment.getArgument('entityType') as EntityType)
                        }
                        existingConfig.setValue(environment.getArgument('value') as String)

                        existingConfig.save(flush: true, failOnError: true)

                        return existingConfig
                    }
                }
            })
        }
        mutation('addListItemProperty', Configuration) {
            argument('key', String)
            argument('entityId', Long)
            argument('entityType', EntityType)
            argument('value', String)
            returns Configuration
            dataFetcher(new CreateEntityDataFetcher(Configuration.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    withTransaction(false) {
                        Configuration existingConfig = new Configuration()
                        existingConfig.setKey(environment.getArgument('key') as String)
                        existingConfig.setEntityId(environment.getArgument('entityId') as Long)
                        existingConfig.setEntityType(environment.getArgument('entityType') as EntityType)
                        existingConfig.setValue(environment.getArgument('value') as String)
                        existingConfig.save(flush: true, failOnError: true)
                        return existingConfig
                    }
                }
            })
        }
    }
}
