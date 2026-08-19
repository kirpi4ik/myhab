package org.myhab.domain

import org.myhab.domain.common.BaseEntity

/**
 * One row per attempt to use a shared link — successful or denied — so an admin can
 * see exactly when each of the allowed actions was consumed, and whether anyone tried
 * and failed.
 *
 * <p>Deliberately has no {@code static graphql} block: {@code GQLSchemaFactory} skips
 * entities without a {@code GraphQLMapping}, so no auto-CRUD mutations are generated and
 * audit rows cannot be forged or deleted over GraphQL. Read access is the hand-written
 * {@code sharedWidgetAudit} query.</p>
 */
class SharedWidgetAudit extends BaseEntity {

    SharedWidget sharedWidget
    String action
    SharedWidgetAuditResult result
    String resultDescription
    String remoteAddress
    String userAgent

    static belongsTo = [sharedWidget: SharedWidget]

    static constraints = {
        sharedWidget nullable: false
        action nullable: false, blank: false, maxSize: 32
        result nullable: false
        resultDescription nullable: true, maxSize: 255
        remoteAddress nullable: true, maxSize: 64
        userAgent nullable: true, maxSize: 512
    }

    static mapping = {
        table '`shared_widget_audit`'
        sharedWidget column: 'shared_widget_id', index: 'shared_widget_audit_widget_idx'
        action column: 'action'
        result column: 'result'
        resultDescription column: 'result_description'
        remoteAddress column: 'remote_address'
        userAgent column: 'user_agent'
        sort tsCreated: 'desc'
    }
}
