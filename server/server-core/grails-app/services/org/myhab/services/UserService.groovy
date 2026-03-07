package org.myhab.services

import org.myhab.domain.Role
import org.myhab.domain.User
import org.myhab.domain.UserRole
import org.myhab.domain.auth.AccessToken
import org.myhab.domain.auth.RefreshToken
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

    /**
     * Invalidate all tokens for a user (access tokens and refresh tokens).
     * Use this when user account is locked, expired, or password is expired.
     * 
     * @param username The username whose tokens should be invalidated
     * @return Number of tokens invalidated
     */
    def invalidateUserTokens(String username) {
        if (!username) {
            log.warn("Cannot invalidate tokens: username is null or empty")
            return 0
        }

        try {
            int accessTokensDeleted = 0
            int refreshTokensDeleted = 0

            // Delete all access tokens for this user
            def accessTokens = AccessToken.findAllByUsername(username)
            accessTokens.each { token ->
                token.delete(flush: true)
                accessTokensDeleted++
            }

            // Delete all refresh tokens by deserializing authentication and checking username
            // Note: RefreshToken doesn't have a direct username field, so we need to check all tokens
            def allRefreshTokens = RefreshToken.list()
            allRefreshTokens.each { refreshToken ->
                try {
                    // Deserialize the authentication to get the username
                    def auth = new ObjectInputStream(new ByteArrayInputStream(refreshToken.authentication)).readObject()
                    def tokenUsername = auth?.principal?.username ?: auth?.name
                    
                    if (tokenUsername == username) {
                        refreshToken.delete(flush: true)
                        refreshTokensDeleted++
                    }
                } catch (Exception ex) {
                    log.warn("Failed to deserialize refresh token authentication: ${ex.message}")
                }
            }

            log.info("Invalidated ${accessTokensDeleted} access tokens and ${refreshTokensDeleted} refresh tokens for user: ${username}")
            return accessTokensDeleted + refreshTokensDeleted

        } catch (Exception ex) {
            log.error("Failed to invalidate tokens for user: ${username}", ex)
            return 0
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
