package org.myhab.services

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.domain.MessageLevel
import org.myhab.domain.MessageState
import org.myhab.domain.Role
import org.myhab.domain.User
import org.myhab.domain.UserMessage
import org.myhab.domain.UserRole

/**
 * Lightweight helper for creating {@link UserMessage} rows from any service.
 *
 * <p>Today the only producer of in-app notifications is the Telegram-bot
 * handler, which writes directly to UserMessage. As more integrations grow
 * (Navimow mower events, scheduled job failures, scenario errors) they all
 * need the same "fan out one message to one user / all admins" primitive
 * without re-implementing the boilerplate. This service is intentionally
 * small — it doesn't add filtering, deduplication or rate-limiting; callers
 * decide when to invoke it.</p>
 */
@Slf4j
@Transactional
class NotificationService {

    /**
     * Persist a single notification for a specific user. Returns the saved
     * UserMessage, or {@code null} if persistence failed (the failure is
     * logged — never thrown — so a notification bug can't break the producer).
     */
    UserMessage notify(User user, MessageLevel level, String subject, String message, String fromSender = 'system') {
        if (user == null) {
            log.warn("notify() called with null user (subject='${subject}') — skipping")
            return null
        }
        try {
            UserMessage um = new UserMessage(
                    subject: trim(subject, 255),
                    fromSender: trim(fromSender, 255),
                    message: message ?: '',
                    level: level ?: MessageLevel.INFO,
                    state: MessageState.NEW,
                    user: user
            )
            um.save(flush: true, failOnError: false)
            if (um.hasErrors()) {
                log.error("Failed to save UserMessage for user=${user.id}: ${um.errors}")
                return null
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
     */
    int notifyAdmins(MessageLevel level, String subject, String message, String fromSender = 'system') {
        Role adminRole = Role.findByAuthority('ROLE_ADMIN')
        if (adminRole == null) {
            log.warn("notifyAdmins: ROLE_ADMIN not found — no users notified (subject='${subject}')")
            return 0
        }
        List<User> admins = UserRole.findAllByRole(adminRole)*.user.findAll { it != null }.unique { it.id }
        int delivered = 0
        admins.each { User u ->
            if (notify(u, level, subject, message, fromSender) != null) delivered++
        }
        return delivered
    }

    private static String trim(String s, int maxLen) {
        if (s == null) return ''
        return s.length() > maxLen ? s.substring(0, maxLen) : s
    }
}
