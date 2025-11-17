package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import org.myhab.config.ConfigProvider
import org.myhab.domain.User
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.myhab.domain.device.DeviceModel
import org.myhab.init.cache.CacheMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Query {
    @Autowired
    HazelcastInstance hazelcastInstance;
    @Autowired
    ConfigProvider configProvider

    def userRolesForUser() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def userId = environment.getArgument("userId")
                def user = User.findById(userId)
                def userRoles = user.authorities
                def response = []
                user.authorities.each {
                    response << [userId: user.id, roleId: it.id]
                }
                return response
            }


        }
    }

    def cache() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def cacheName = environment.getArgument("cacheName")
                def cacheKey = environment.getArgument("cacheKey")
                // Convert cacheKey to String to match mutation's String.valueOf()
                def cachedValue = hazelcastInstance.getMap(cacheName).get(String.valueOf(cacheKey))
                // Return actual null instead of string "null" when cache is empty
                def expireOn = cachedValue ? cachedValue["expireOn"] : null
                return [cacheName: cacheName, cacheKey: cacheKey, cachedValue: expireOn]

            }
        }
    }

    def cacheAll() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String cacheName = environment.getArgument("cacheName")
                def result = []
                
                if (cacheName) {
                    // If specific cache name is provided, only query that cache
                    hazelcastInstance.getMap(cacheName).entrySet().each { entry ->
                        // Return actual null instead of string "null" when cache is empty
                        def expireOn = entry.value ? entry.value["expireOn"] : null
                        result << [cacheName: cacheName, cacheKey: entry.key, cachedValue: expireOn]
                    }
                } else {
                    // If no cache name provided, query all caches
                    CacheMap.values().each { cMap ->
                        hazelcastInstance.getMap(cMap.name).entrySet().each { entry ->
                            // Return actual null instead of string "null" when cache is empty
                            def expireOn = entry.value ? entry.value["expireOn"] : null
                            result << [cacheName: cMap.name, cacheKey: entry.key, cachedValue: expireOn]
                        }
                    }
                }
                return result
            }
        }
    }

    def config() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def key = environment.getArgument("key")
                def config = configProvider.get(String, key)
                return config
            }
        }
    }

    def deviceModelList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return DeviceModel.values()
            }
        }
    }

}
