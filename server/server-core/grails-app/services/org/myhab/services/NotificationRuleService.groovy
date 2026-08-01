package org.myhab.services

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.domain.MessageState
import org.myhab.domain.NotificationRule
import org.myhab.domain.RuleMatchType
import org.myhab.domain.User
import org.myhab.domain.UserMessage

import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Evaluates per-user {@link NotificationRule}s.
 *
 * <p>Used from two places: the write path ({@code NotificationService.persist}) to file an
 * incoming message directly as READ/ARCHIVE, and the {@code notificationRuleCreate} mutation
 * to retroactively clear the backlog a new rule would have caught.</p>
 */
@Slf4j
@Transactional
class NotificationRuleService {

    /** Compiled-regex cache, keyed by pattern. Rules change rarely; messages arrive often. */
    private static final Map<String, Pattern> COMPILED = new ConcurrentHashMap<>()

    /** The first rule of this user matching the message, or null when nothing matches. */
    NotificationRule firstMatching(User user, String fromSender, String subject, String dedupKey) {
        if (user == null) {
            return null
        }
        return NotificationRule.findAllByUser(user).find { matches(it, fromSender, subject, dedupKey) }
    }

    /**
     * File every NEW message of this user that the rule catches. Only NEW is touched — messages
     * the user deliberately read or archived are left alone.
     *
     * @return how many rows were updated
     */
    int applyToExisting(User user, NotificationRule rule) {
        if (user == null || rule == null) {
            return 0
        }
        // Filtered in memory for every match type: SUBJECT_REGEX has no clean GORM criteria
        // equivalent, and a SQL `like` on a key prefix would treat the `_` in keys such as
        // `navimow.5.low_battery` as a wildcard. The NEW backlog is small enough that it's free.
        List<UserMessage> matched = UserMessage.findAllByUserAndState(user, MessageState.NEW)
                .findAll { matches(rule, it.fromSender, it.subject, it.dedupKey) }
        matched.each { UserMessage um ->
            um.state = rule.targetState
            um.save(failOnError: false)
        }
        return matched.size()
    }

    boolean matches(NotificationRule rule, String fromSender, String subject, String dedupKey) {
        switch (rule.matchType) {
            case RuleMatchType.SENDER:
                return fromSender != null && fromSender == rule.pattern
            // A producer that supplies no dedupKey simply never matches a KEY_PREFIX rule —
            // SENDER and SUBJECT_REGEX remain available for it.
            case RuleMatchType.KEY_PREFIX:
                return dedupKey != null && dedupKey.startsWith(rule.pattern)
            case RuleMatchType.SUBJECT_REGEX:
                return subject != null && regexMatches(rule.pattern, subject)
            default:
                return false
        }
    }

    /** An unparseable pattern never matches and never throws — a bad rule must not break delivery. */
    private boolean regexMatches(String pattern, String subject) {
        try {
            return COMPILED.computeIfAbsent(pattern, { String p -> Pattern.compile(p) })
                    .matcher(subject).find()
        } catch (Exception ex) {
            log.warn("Invalid notification rule regex '${pattern}': ${ex.message}")
            return false
        }
    }
}
