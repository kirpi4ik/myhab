package org.myhab.init.cache

enum CacheMap {
    EXPIRE("expiring"),
    TOKENS("tokens"),
    // CSRF state tokens for OAuth flows that bounce out to a third-party
    // authorize page and come back via /auth/external/callback. Entries are
    // short-lived (~10 minutes) and removed on first consumption.
    OAUTH_STATE("oauth_state"),
    // Per-key cooldown windows for `NotificationService.notify` / `notifyAdmins`.
    // Entries are stored with a per-entry TTL via IMap.put(key, value, ttl, …)
    // so the cooldown window self-clears without a dedicated reaper job.
    // Cluster-replicated so a notification fired on node A within the window
    // suppresses a duplicate on node B.
    NOTIFICATION_DEDUP("notification_dedup")

    String name

    CacheMap(def name) {
        this.name = name
    }
}