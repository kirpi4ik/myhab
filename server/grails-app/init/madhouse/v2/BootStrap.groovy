package madhouse.v2

import eu.devexpert.madhouse.Role
import eu.devexpert.madhouse.User
import eu.devexpert.madhouse.UserRole

import javax.transaction.Transactional

class BootStrap {

    def initService

    def init = { servletContext ->
//        initService.initUsers()
    }
    def destroy = {
    }

}
