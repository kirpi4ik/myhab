package eu.devexpert.madhouse

import grails.gorm.transactions.Transactional

@Transactional
class InitService {

    def initUsers() {
        Role admin = new Role("ROLE_ADMIN").save(flush: true, failOnError: true)
        User user = new User("admin", "admin").save(flush: true, failOnError: true)
        UserRole.create(user, admin, true).save(flush: true, failOnError: true)
    }
}
