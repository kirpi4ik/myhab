package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.ZoneAccessToken
import grails.gorm.transactions.Transactional

@Transactional
class ZoneAccessService {

    def isValidToken(def tokenString){
        ZoneAccessToken.where {
            token == token
        }
    }
    def serviceMethod() {

    }
}
