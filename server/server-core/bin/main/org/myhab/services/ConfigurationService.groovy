package org.myhab.services

import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import org.myhab.domain.common.Event
import org.myhab.domain.events.TopicName

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
