package org.myhab.services

import org.myhab.domain.Role
import org.myhab.domain.User
import org.myhab.domain.UserRole
import org.myhab.graphql.fetchers.DefaultDataFetcher
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j

@Slf4j
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
            log.error("Failed to update user roles", ex)
            return [success: false]

        }
    }

    def userHasRole(String username, String roleName) {
        def myHabUser = User.findByUsername(username)
        return myHabUser != null ? myHabUser.authorities.stream().anyMatch { role -> role.authority == roleName }.booleanValue() : false
    }

    def tgUserHasAnyRole(String username, List roleNames) {
        def myHabUser = User.findByTelegramUsername(username)
        if (myHabUser == null) {
            return false
        }
        
        // ROLE_ADMIN has access to everything
        if (myHabUser.authorities.stream().anyMatch { role -> role.authority == "ROLE_ADMIN" }.booleanValue()) {
            return true
        }
        
        // Check if user has any of the required roles
        return roleNames.any { roleName -> 
            myHabUser.authorities.stream().anyMatch { role -> role.authority == roleName }.booleanValue() 
        }
    }

}
