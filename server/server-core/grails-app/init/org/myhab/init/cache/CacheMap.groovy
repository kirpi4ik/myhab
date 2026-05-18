package org.myhab.init.cache

enum CacheMap {
    EXPIRE("expiring"),
    TOKENS("tokens"),
    // CSRF state tokens for OAuth flows that bounce out to a third-party
    // authorize page and come back via /auth/external/callback. Entries are
    // short-lived (~10 minutes) and removed on first consumption.
    OAUTH_STATE("oauth_state")

    String name

    CacheMap(def name) {
        this.name = name
    }
}