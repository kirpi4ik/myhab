package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.Role
import eu.devexpert.madhouse.domain.User
import eu.devexpert.madhouse.domain.UserRole
import eu.devexpert.madhouse.graphql.fetchers.DefaultDataFetcher
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetchingEnvironment

@Transactional
class UserService extends DefaultDataFetcher {

    @Override
    Object get(DataFetchingEnvironment environment) throws Exception {
        try {
            def saveUserRoles = environment.getArgument("input")
            def existingUser = User.findByUid(saveUserRoles.userUid)
            def newRoleIds = []
            saveUserRoles.userRoles.each {
                newRoleIds << Long.valueOf("${it.roleId}")
            }
            DetachedCriteria<UserRole> uRoles = UserRole.where {
                user == existingUser
            }
            //delete old roles which are not present in request
            uRoles.list().each { oldRole ->
                if (!newRoleIds.contains(oldRole.roleId)) {
                    UserRole.deleteAll(oldRole)
                }
            }

            //Add new roles which not exist yet
            newRoleIds.each { id ->
                if (!UserRole.exists(existingUser.id, id as long)) {
                    UserRole.create(existingUser, Role.findById(id))
                }
            }
            return [success: true]

        } catch (Exception ex) {
            ex.printStackTrace()
            log.error(ex.message)
            return [success: false]

        }
    }

}
