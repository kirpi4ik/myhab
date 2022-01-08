package eu.devexpert.madhouse.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import eu.devexpert.madhouse.domain.User
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
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

}
