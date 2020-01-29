package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.common.BaseEntity
import grails.gorm.transactions.Transactional

@Transactional
class ConfigurationService {

    def <T> T getProperty(BaseEntity entity, T type, String key) {

    }

    def <T> Set<T> getList(BaseEntity entity, T type, String key) {

    }

    def saveProperty(BaseEntity entity, String key, Object value) {

    }

    def saveList(BaseEntity entity, String key, Set<Object> values) {

    }
}
