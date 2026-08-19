package org.myhab.domain

/**
 * How a {@link NotificationRule} decides that an incoming message is "similar".
 *
 * <p>Producer-agnostic by design: SENDER and SUBJECT_REGEX work for every message,
 * while KEY_PREFIX gives finer granularity when the producer supplied a dedupKey.</p>
 */
enum RuleMatchType {
    /** fromSender equals the pattern exactly. */
    SENDER,
    /** dedupKey starts with the pattern, e.g. `navimow.5.state` mutes every state change. */
    KEY_PREFIX,
    /** subject matches the pattern as a regular expression. */
    SUBJECT_REGEX
}
