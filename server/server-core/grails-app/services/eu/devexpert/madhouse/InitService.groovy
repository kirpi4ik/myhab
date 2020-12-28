package eu.devexpert.madhouse

import eu.devexpert.madhouse.domain.Role
import eu.devexpert.madhouse.domain.User
import eu.devexpert.madhouse.domain.UserRole
import eu.devexpert.madhouse.domain.auth.Client
import grails.gorm.transactions.Transactional

@Transactional
class InitService {

    def initClient(){
        new Client(
                clientId: 'google-madhouse-control-light',
                clientSecret: '13DrEwfffff4fwwefwefweefwe',
                authorizedGrantTypes: ['authorization_code', 'refresh_token', 'implicit', 'password', 'client_credentials'],
                authorities: ['ROLE_CLIENT'],
                scopes: ['read', 'profile'],
                redirectUris: []
        ).save(flush: true, failOnError: true)
    }
    def initUsers() {
        Role admin = new Role("ROLE_ADMIN").save(flush: true, failOnError: true)
        User user = new User("admin", "admin").save(flush: true, failOnError: true)
        UserRole.create(user, admin, true).save(flush: true, failOnError: true)
    }
}
