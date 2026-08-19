package org.myhab.services

import com.hazelcast.core.HazelcastInstance
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.domain.MessageLevel
import org.myhab.domain.MessageState
import org.myhab.domain.NotificationRule
import org.myhab.domain.Role
import org.myhab.domain.User
import org.myhab.domain.UserMessage
import org.myhab.domain.UserRole
import org.myhab.init.cache.CacheMap

import java.util.concurrent.TimeUnit

/**
 * Lightweight helper for creating {@link UserMessage} rows from any service.
 *
 * <p>Producers today are the Navimow sync job and the MQTT notify bridge
 * ({@code myhab/<source>/notify}); more will follow. They all need the same
 * "fan out one message to one user / all admins" primitive without
 * re-implementing the boilerplate.</p>
 *
 * <p><b>Muting</b>: every row is written by {@link #persist}, which consults the
 * recipient's {@link org.myhab.domain.NotificationRule}s. A match files the message
 * directly as READ/ARCHIVE and skips the Web Push. That is producer-independent —
 * any current or future caller is mutable without touching it.</p>
 *
 * <p><b>Dedup</b>: callers pass an optional {@code dedupKey} and {@code
 * cooldownMinutes}; if a notification with that key was fired within the
 * cooldown window, the call returns null without writing. Backed by the
 * cluster-replicated {@link CacheMap#NOTIFICATION_DEDUP} Hazelcast map with
 * per-entry TTL so the cooldown self-clears.</p>
 *
 * <p>Without a {@code dedupKey} the service writes every call — preserving
 * the original "callers decide when to invoke it" semantics for legacy sites
 * that don't yet opt into dedup.</p>
 */
@Slf4j
@Transactional
class NotificationService {

    /** Default cooldown when a caller supplies a dedupKey but no explicit window. */
    static final int DEFAULT_COOLDOWN_MINUTES = 60

    HazelcastInstance hazelcastInstance
    WebPushService webPushService
    NotificationRuleService notificationRuleService

    /**
     * Persist a single notification for a specific user. Returns the saved
     * UserMessage, or {@code null} if persistence failed OR the dedup
     * cooldown suppressed it (the failure is logged — never thrown — so a
     * notification bug can't break the producer).
     *
     * @param dedupKey         When non-null, enables cooldown-based dedup.
     *                         A typical key carries the source + entity id,
     *                         e.g. {@code "navimow.42.token_expired"}.
     * @param cooldownMinutes  TTL on the dedup entry. Defaults to 60 min.
     *                         Ignored when {@code dedupKey} is null.
     */
    UserMessage notify(User user, MessageLevel level, String subject, String message,
                       String fromSender = 'system',
                       String dedupKey = null, int cooldownMinutes = DEFAULT_COOLDOWN_MINUTES) {
        if (user == null) {
            log.warn("notify() called with null user (subject='${subject}') — skipping")
            return null
        }
        if (dedupKey != null && isOnCooldown(dedupKey)) {
            log.debug("Notification suppressed (cooldown) key=${dedupKey} user=${user.id} subject='${subject}'")
            return null
        }
        UserMessage um = persist(user, level, subject, message, fromSender, dedupKey)
        if (um != null && dedupKey != null) {
            markFired(dedupKey, cooldownMinutes)
        }
        return um
    }

    /**
     * Write exactly one row. Returns null only when nothing was written.
     *
     * <p>This is the single place a UserMessage is created, so it is also where per-user
     * {@link org.myhab.domain.NotificationRule}s are applied: a matching rule files the
     * message straight as READ/ARCHIVE and suppresses the Web Push. The row is still
     * stored — muting hides a message, it never loses it.</p>
     *
     * <p>Separate from {@link #notify} so {@link #notifyAdmins} can fan out one row per admin
     * while enforcing the cooldown exactly once for the whole call.</p>
     */
    private UserMessage persist(User user, MessageLevel level, String subject, String message,
                                String fromSender, String dedupKey) {
        try {
            NotificationRule rule = notificationRuleService?.firstMatching(user, fromSender, subject, dedupKey)
            UserMessage um = new UserMessage(
                    subject: trim(subject, 255),
                    fromSender: trim(fromSender, 255),
                    message: message ?: '',
                    level: level ?: MessageLevel.INFO,
                    state: rule ? rule.targetState : MessageState.NEW,
                    // trim() maps null to '' — keep a keyless message as SQL NULL.
                    dedupKey: dedupKey ? trim(dedupKey, 255) : null,
                    user: user
            )
            um.save(flush: true, failOnError: false)
            if (um.hasErrors()) {
                log.error("Failed to save UserMessage for user=${user.id}: ${um.errors}")
                return null
            }
            if (rule != null) {
                log.debug("Message filed as ${rule.targetState} by rule ${rule.id} (${rule.matchType}:${rule.pattern}) user=${user.id}")
                return um
            }
            // Best-effort native push (Web Push / VAPID). Never let a push bug
            // break the producer — sendToUser is itself fire-and-forget, but
            // guard the call site too.
            try {
                webPushService?.sendToUser(user, um)
            } catch (Exception ex) {
                log.warn("Web push dispatch failed for user=${user.id}: ${ex.message}")
            }
            return um
        } catch (Exception ex) {
            log.error("Exception writing UserMessage for user=${user.id}: ${ex.message}", ex)
            return null
        }
    }

    /**
     * Fan a single notification out to every user holding ROLE_ADMIN. Returns
     * the number of successfully-persisted rows. No-ops cleanly when there are
     * no admin users (e.g. a fresh install during setup).
     *
     * <p>Dedup is enforced ONCE per call — checked before the fan-out, marked
     * after the first successful delivery. Without this, the first admin's
     * notify would mark the cooldown and subsequent admins in the same call
     * would receive nothing.</p>
     */
    int notifyAdmins(MessageLevel level, String subject, String message,
                     String fromSender = 'system',
                     String dedupKey = null, int cooldownMinutes = DEFAULT_COOLDOWN_MINUTES) {
        if (dedupKey != null && isOnCooldown(dedupKey)) {
            log.debug("Admin notification suppressed (cooldown) key=${dedupKey} subject='${subject}'")
            return 0
        }
        Role adminRole = Role.findByAuthority('ROLE_ADMIN')
        if (adminRole == null) {
            log.warn("notifyAdmins: ROLE_ADMIN not found — no users notified (subject='${subject}')")
            return 0
        }
        List<User> admins = UserRole.findAllByRole(adminRole)*.user.findAll { it != null }.unique { it.id }
        int delivered = 0
        admins.each { User u ->
            // persist() rather than notify(): dedup is already enforced at the top of this
            // method, and the marker is set once after the fan-out so every admin gets the
            // message. Going through notify() would mark the cooldown on the first admin and
            // silently suppress the rest — but the key still has to reach every row.
            if (persist(u, level, subject, message, fromSender, dedupKey) != null) delivered++
        }
        if (delivered > 0 && dedupKey != null) {
            markFired(dedupKey, cooldownMinutes)
        }
        return delivered
    }

    // ----------------------------------------------------------------------
    // Cooldown helpers (Hazelcast IMap with per-entry TTL)
    // ----------------------------------------------------------------------

    private boolean isOnCooldown(String dedupKey) {
        try {
            return hazelcastInstance?.getMap(CacheMap.NOTIFICATION_DEDUP.name)?.containsKey(dedupKey) ?: false
        } catch (Exception ex) {
            // If Hazelcast is unavailable, fail open — we'd rather send a
            // duplicate than swallow a real alert because of a cache outage.
            log.warn("Notification dedup lookup failed for key=${dedupKey}: ${ex.message}")
            return false
        }
    }

    private void markFired(String dedupKey, int cooldownMinutes) {
        try {
            long ttlSeconds = Math.max(1L, cooldownMinutes as long) * 60L
            hazelcastInstance?.getMap(CacheMap.NOTIFICATION_DEDUP.name)
                    ?.put(dedupKey, System.currentTimeMillis(), ttlSeconds, TimeUnit.SECONDS)
        } catch (Exception ex) {
            log.warn("Notification dedup mark failed for key=${dedupKey}: ${ex.message}")
        }
    }

    private static String trim(String s, int maxLen) {
        if (s == null) return ''
        return s.length() > maxLen ? s.substring(0, maxLen) : s
    }
}
