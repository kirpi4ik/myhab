package eu.devexpert.madhouse.services

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.common.Event
import eu.devexpert.madhouse.domain.events.TopicName
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional

@Transactional
class ConfigurationService implements EventPublisher {

    def changed(Configuration config) {
        publish(TopicName.EVT_CFG_VALUE_CHANGED.id(), new Event().with {
            p0 = TopicName.EVT_CFG_VALUE_CHANGED.id()
            p1 = EntityType.CONFIG.name()
            p2 = config.entityType.name()
            p3 = "${config.entityId}"
            p4 = "${config.key}"
            p5 = "${config.value}"
            p6 = "system"
            it
        })
    }
}
