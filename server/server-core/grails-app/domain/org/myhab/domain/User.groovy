package org.myhab.domain

import org.myhab.domain.common.BaseEntity
import grails.rest.Resource
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher
import org.myhab.domain.job.Job
import org.myhab.services.UserService

@Resource(uri = '/user')
@EqualsAndHashCode(includes = 'username')
@ToString(includes = 'username', includeNames = true, includePackage = false)
class User extends BaseEntity {
    private static final long serialVersionUID = 1
    transient springSecurityService
    transient UserService userService

    String username
    String password
    String phoneNr
    Boolean enabled = true
    Boolean accountExpired = false
    Boolean accountLocked = false
    Boolean passwordExpired = false

    String email
    String firstName
    String lastName
    String name
    String telegramUsername
    Set<PeripheralAccessToken> peripheralAccessTokens
    /** User avatar image (PNG/JPEG, max 3KB). Not exposed via GraphQL; use REST GET /api/users/:id/avatar */
    byte[] avatar
    /** Preferred UI language (BCP-47 base code, e.g. 'en' / 'ro'). Null = follow browser. */
    String language
    /** Preferred display timezone (IANA id, e.g. 'Europe/Chisinau'). Null = follow browser. */
    String timezone

    User(String username, String password) {
        this()
        this.username = username
        this.password = password
    }

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    /**
     * Before update event - automatically invalidate tokens if account is locked, expired, or password expired
     */
    @Override
    void beforeUpdate() {
        super.beforeUpdate()
        // Get the persisted (old) state of the user
        def oldUser = User.get(this.id)
        
        // Check if any security flags changed to true
        boolean shouldInvalidateTokens = false
        
        if (this.accountLocked && (!oldUser?.accountLocked)) {
            shouldInvalidateTokens = true
            log.info("User ${this.username} account locked - will invalidate tokens")
        }
        
        if (this.accountExpired && (!oldUser?.accountExpired)) {
            shouldInvalidateTokens = true
            log.info("User ${this.username} account expired - will invalidate tokens")
        }
        
        if (this.passwordExpired && (!oldUser?.passwordExpired)) {
            shouldInvalidateTokens = true
            log.info("User ${this.username} password expired - will invalidate tokens")
        }
        
        // Invalidate tokens if needed
        if (shouldInvalidateTokens && userService) {
            try {
                userService.invalidateUserTokens(this.username)
            } catch (Exception ex) {
                log.error("Failed to invalidate tokens for user ${this.username} in beforeUpdate", ex)
            }
        }
    }

    static graphql = GraphQLMapping.lazy {
        exclude('avatar')  // Served via REST GET /api/users/:id/avatar only
        query('userById', User) {
            argument('id', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    User.findById(environment.getArgument('id'))
                }
            })
        }
        mutation('userDeleteCascade', "DeleteResponseCustom") {
            argument('id', Long)
            returns {
                field('error', String)
                field('success', Boolean)
            }
            dataFetcher(new DeleteEntityDataFetcher(User.gormPersistentEntity) {
                @Override
                Object get(DataFetchingEnvironment environment) throws Exception {
                    withTransaction(false) {
                        Long userId = environment.getArgument("id") as Long
                        def currentUser = User.findById(userId)
                        
                        // Remove user roles
                        UserRole.where {
                            user == currentUser
                        }.deleteAll()
                        
                        // Remove user's favorite jobs
                        UserFavJobJoin.removeAllForUser(userId)
                        
                        currentUser.delete()
                    }
                    return [success: true, error: null]
                }
            })
        }
    }

    static constraints = {
        password nullable: false, blank: false, password: true
        username nullable: false, blank: false, unique: true
        firstName nullable: true
        lastName nullable: true
        email nullable: true
        phoneNr nullable: true
        telegramUsername nullable: true
        avatar nullable: true, maxSize: 3 * 1024
        language nullable: true
        timezone nullable: true
    }

    static hasMany = [favJobs: Job, peripheralAccessTokens: PeripheralAccessToken]

    static mapping = {
        table '`users`'
        password column: '`password`'
        name formula: 'concat(FIRST_NAME,\' \',LAST_NAME)'
        version false
        autowire true
        favJobs joinTable: [name: "users_fav_jobs", key: 'user_id']
        avatar type: 'binary'
    }


}
