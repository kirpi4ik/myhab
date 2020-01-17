package eu.devexpert.madhouse.gql.fetchers

import eu.devexpert.madhouse.domain.User
import eu.devexpert.madhouse.domain.UserRole
import eu.devexpert.madhouse.domain.infra.Zone
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Query {

    def userRolesForUser() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def userUid = environment.getArgument("userUid")
                def user = User.findByUid(userUid)
                def userRoles = user.authorities
                def response = []
                user.authorities.each {
                    response << [userId: user.id, roleId: it.id]
                }
                return response
            }


        }
    }

}
