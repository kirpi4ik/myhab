package org.myhab.domain

import org.myhab.domain.common.BaseEntity

/**
 * A per-user filter: messages matching {@code pattern} skip the inbox and the Web Push,
 * and are filed straight as {@code targetState} (READ or ARCHIVE).
 *
 * <p>Evaluated in {@code NotificationService.persist()} — the single place a UserMessage
 * is ever created — so it applies to every producer, present and future.</p>
 *
 * <p>Deliberately NOT GraphQL-exposed (no {@code static graphql}): the auto-generated GORM
 * CRUD is unscoped, which would let any authenticated user read and delete another user's
 * rules. Access goes through the hand-written, owner-scoped fetchers instead.</p>
 */
class NotificationRule extends BaseEntity {

    User user
    RuleMatchType matchType
    String pattern
    MessageState targetState

    static belongsTo = [user: User]

    static constraints = {
        matchType nullable: false
        pattern nullable: false, blank: false, maxSize: 255
        targetState nullable: false, validator: { MessageState st ->
            // NEW would make the rule a no-op; only filing states make sense here.
            if (!(st in [MessageState.READ, MessageState.ARCHIVE])) {
                return 'notificationRule.targetState.invalid'
            }
        }
    }

    static mapping = {
        table '`notification_rules`'
        user column: 'user_id'
        sort tsCreated: 'desc'
    }
}
