package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import org.myhab.config.ConfigProvider
import org.myhab.domain.User
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
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
                def cachedValue = hazelcastInstance.getMap(cacheName).get(cacheKey)
                return [cacheName: cacheName, cacheKey: cacheKey, cachedValue: "${cachedValue ? cachedValue["expireOn"] : null}"]

            }
        }
    }

    def cacheAll() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String cacheName = environment.getArgument("cacheName")
                def result = []
                CacheMap.values().each { cMap ->
                    def cName = cacheName ?: cMap.name
                    hazelcastInstance.getMap(cName).entrySet().each { entry ->
                        result << [cacheName: cName, cacheKey: entry.key, cachedValue: "${entry.value ? entry.value["expireOn"] : null}"]
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

}
