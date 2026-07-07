package org.myhab.domain

import org.myhab.domain.common.BaseEntity

/**
 * A Web Push (VAPID) subscription for one browser/device of one {@link User}.
 *
 * <p>Created when a user opts into native notifications from the PWA
 * ({@code pushSubscribe} mutation) and consumed by {@code WebPushService} to
 * deliver {@link UserMessage} pushes even while the app is closed. A single
 * user may hold many subscriptions (one per browser/device); {@code endpoint}
 * is the unique browser-issued URL and the natural key for upsert/prune.</p>
 *
 * <p>Deliberately <b>not</b> GraphQL-exposed (no {@code static graphql}) — the
 * p256dh/auth material is a client secret and must never leak through the API.</p>
 */
class PushSubscription extends BaseEntity {

    String endpoint
    String p256dhKey
    String authKey
    String userAgent
    User user

    static belongsTo = [user: User]

    static constraints = {
        endpoint nullable: false, blank: false, unique: true, maxSize: 2048
        p256dhKey nullable: false, blank: false
        authKey nullable: false, blank: false
        userAgent nullable: true, maxSize: 512
        user nullable: false
    }

    static mapping = {
        table '`push_subscription`'
        endpoint type: 'text'
        user column: 'user_id'
        sort tsCreated: 'desc'
    }
}
