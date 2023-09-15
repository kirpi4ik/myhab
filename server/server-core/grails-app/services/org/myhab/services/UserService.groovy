package org.myhab.services

import org.myhab.domain.Role
import org.myhab.domain.User
import org.myhab.domain.UserRole
import org.myhab.graphql.fetchers.DefaultDataFetcher
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetchingEnvironment

@Transactional
class UserService extends DefaultDataFetcher {

    @Override
    Object get(DataFetchingEnvironment environment) throws Exception {
        try {
            def saveUserRoles = environment.getArgument("input")
            def existingUser = User.findById(saveUserRoles.userId)
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

    def userHasRole(String username, String roleName) {
        def myHabUser = User.findByUsername(username)
        return myHabUser != null ? myHabUser.authorities.stream().anyMatch { role -> role.authority == roleName }.booleanValue() : false
    }

    def tgUserHasAnyRole(String username, List roleNames) {
        def myHabUser = User.findByTelegramUsername(username)
        return myHabUser != null ? roleNames.any { roleName -> myHabUser.authorities.stream().anyMatch { role -> role.authority == roleName }.booleanValue() } : false
    }

}
