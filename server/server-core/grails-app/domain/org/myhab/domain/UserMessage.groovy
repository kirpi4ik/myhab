package org.myhab.domain

import org.myhab.domain.common.BaseEntity
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class UserMessage extends BaseEntity {

    String subject
    String fromSender
    String message
    MessageLevel level = MessageLevel.INFO
    MessageState state = MessageState.NEW
    User user
    /**
     * Producer-supplied stability key (e.g. `navimow.5.state.mowing`, `mqtt.gate_controller.tag.99`).
     * Match key for NotificationRule KEY_PREFIX rules; null when the producer supplied none.
     */
    String dedupKey

    static belongsTo = [user: User]

    static graphql = GraphQLMapping.lazy {}

    static constraints = {
        subject nullable: false, blank: false, maxSize: 255
        fromSender nullable: false, blank: false, maxSize: 255
        message nullable: false, blank: false
        level nullable: false
        state nullable: false
        user nullable: false
        dedupKey nullable: true, maxSize: 255
    }

    static mapping = {
        table '`user_messages`'
        message type: 'text'
        user column: 'user_id'
        sort tsCreated: 'desc'
    }
}
