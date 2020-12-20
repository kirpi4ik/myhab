package eu.devexpert.madhouse

import eu.devexpert.madhouse.domain.Role
import eu.devexpert.madhouse.domain.User
import eu.devexpert.madhouse.domain.UserRole
import grails.gorm.transactions.Transactional

@Transactional
class InitService {

    def initClient(){
        new eu.devexpert.madhouse.domain.auth.Client(
                clientId: 'amzn1.application-oa2-client.a5d9d93ae38b4eaeb373a65598c9a8d7',
                authorizedGrantTypes: ['authorization_code', 'refresh_token', 'implicit', 'password', 'client_credentials'],
                authorities: ['ROLE_CLIENT'],
                scopes: ['read', 'profile'],
                redirectUris: ['https://pitangui.amazon.com/api/skill/link/M1COE12M7TWH7K', 'https://alexa.amazon.co.jp/api/skill/link/M1COE12M7TWH7K', 'https://layla.amazon.com/api/skill/link/M1COE12M7TWH7K']
        ).save(flush: true)
    }
    def initUsers() {
        Role admin = new Role("ROLE_ADMIN").save(flush: true, failOnError: true)
        User user = new User("admin", "admin").save(flush: true, failOnError: true)
        UserRole.create(user, admin, true).save(flush: true, failOnError: true)
    }
}
