package org.myhab.domain.events

/**
 * Structured origin of an audit-log ({@code event_log}) row — replaces the
 * free-form {@code p3} source strings ("scenario_service", "mweb",
 * "Voice assistant: …") that made the audit trail hard to query.
 *
 * <p>Stored in {@code EventData.p3} as the enum name. The initiating user (when
 * one exists) is recorded separately in {@code EventData.p6} (actor).</p>
 */
enum AuditSource {
    SCENARIO,        // DSL scenario / scheduled or event-triggered job
    VOICE,           // voice assistant
    TELEGRAM,        // telegram bot
    WEB_UI,          // manual control from the web client
    MQTT,            // device-reported (confirmed) state change
    SCHEDULER,       // scheduler-internal action
    ACCESS_CONTROL,  // access-token / door-unlock
    SYSTEM           // unattributed / fallback

    static AuditSource from(def value) {
        if (value instanceof AuditSource) {
            return value
        }
        if (value instanceof CharSequence) {
            return values().find { it.name().equalsIgnoreCase(value.toString()) } ?: SYSTEM
        }
        return SYSTEM
    }
}
