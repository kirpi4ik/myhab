package org.myhab.domain

import org.myhab.domain.common.BaseEntity
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class SharedWidget extends BaseEntity {

    String token
    String pin
    SharedWidgetType widgetType
    String peripheralId
    Date shareStartDate
    Date shareExpireDate
    int actionsAllowed = 5
    int actionsUsed = 0
    SharedWidgetState state = SharedWidgetState.VALID
    String description
    String stateDescription
    String createdByUsername

    static graphql = GraphQLMapping.lazy {
        exclude('pin')
    }

    static constraints = {
        token nullable: false, blank: false, unique: true, maxSize: 64
        pin nullable: true, maxSize: 64
        widgetType nullable: false
        peripheralId nullable: false, blank: false
        shareStartDate nullable: false
        shareExpireDate nullable: false
        actionsAllowed nullable: false, min: 1
        actionsUsed nullable: false, min: 0
        state nullable: false
        description nullable: true, maxSize: 500
        stateDescription nullable: true, maxSize: 500
        createdByUsername nullable: false, blank: false, maxSize: 255
    }

    static mapping = {
        table '`shared_widgets`'
        token column: 'token'
        pin column: 'pin'
        widgetType column: 'widget_type'
        peripheralId column: 'peripheral_id'
        shareStartDate column: 'share_start_date'
        shareExpireDate column: 'share_expire_date'
        actionsAllowed column: 'actions_allowed'
        actionsUsed column: 'actions_used'
        state column: 'state'
        description column: 'description', type: 'text'
        stateDescription column: 'state_description', type: 'text'
        createdByUsername column: 'created_by_username'
        sort tsCreated: 'desc'
    }
}
