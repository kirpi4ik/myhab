package org.myhab.services

import grails.gorm.transactions.Transactional
import org.myhab.domain.User

@Transactional
class TelegramService {

    def userService


    boolean validTGUser(String tgUsername) {
        return User.findByTelegramUsername(tgUsername) != null
    }
}
