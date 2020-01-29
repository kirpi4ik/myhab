package eu.devexpert.madhouse.domain

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.job.Job
import grails.rest.Resource
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping
import org.grails.gorm.graphql.fetcher.impl.DeleteEntityDataFetcher

@Resource(uri = '/user')
@EqualsAndHashCode(includes = 'username')
@ToString(includes = 'username', includeNames = true, includePackage = false)
class User extends BaseEntity {
    private static final long serialVersionUID = 1
    transient springSecurityService

    String username
    String password
    Boolean enabled = true
    Boolean accountExpired = false
    Boolean accountLocked = false
    Boolean passwordExpired = false

    String email
    String firstName
    String lastName
    String name

    User(String username, String password) {
        this()
        this.username = username
        this.password = password
    }

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static graphql = GraphQLMapping.lazy {
        query('userByUid', User) {
            argument('uid', String)
            dataFetcher(new DataFetcher() {
                @Override
                Object get(DataFetchingEnvironment environment) {
                    User.findByUid(environment.getArgument('uid'))
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
                        def currentUser = User.findById(environment.getArgument("id"))
                        UserRole.where {
                            user == currentUser
                        }.deleteAll()
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
    }

    static hasMany = [favJobs: Job]

    static mapping = {
        table '`users`'
        password column: '`password`'
        name formula: 'concat(FIRST_NAME,\' \',LAST_NAME)'
        version false
        autowire true
        favJobs joinTable: [name: "users_fav_jobs", key: 'user_id']
    }


}
