package org.myhab.services

import grails.events.annotation.Subscriber
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.myhab.domain.MessageLevel

/**
 * Turns `myhab/&lt;source&gt;/notify` MQTT messages into {@link org.myhab.domain.UserMessage}
 * rows, so any device can raise an in-app notification without a server change.
 *
 * <p>Payload: {@code {"subject":"…","message":"…","level":"INFO|WARN|ERROR",
 * "dedupKey":"…","cooldown":&lt;minutes&gt;}}. Only {@code subject} is required.
 * {@code fromSender} is always the &lt;source&gt; topic segment, never read from the
 * payload — the topic is the only identity the broker authenticated.</p>
 *
 * <p>This sits off the port-value path on purpose: PortValueService skips writes whose
 * value is unchanged, so the same card scanned twice would produce nothing. A port value
 * models state; a notification is an occurrence.</p>
 */
@Slf4j
class NotificationBridgeService {

    static final int MAX_PAYLOAD = 4096

    NotificationService notificationService

    // No @Transactional here: this method touches no GORM of its own, and
    // NotificationService is @Transactional at class level so it opens its own.
    @Subscriber('evt_user_notification')
    void onMqttNotification(event) {
        // publish() is synchronous, so this runs on the MQTT handler thread — a bad
        // payload must never escape this method and kill the ingestion pipeline.
        try {
            String source = event.data.p2
            String raw = event.data.p5
            if (!raw || raw.length() > MAX_PAYLOAD) {
                log.warn("notify/${source}: empty or oversized payload (${raw?.length()})")
                return
            }
            def p = new JsonSlurper().parseText(raw)
            if (!(p instanceof Map) || !p.subject?.toString()?.trim()) {
                log.warn("notify/${source}: not a JSON object, or missing subject")
                return
            }
            String subject = p.subject.toString()
            // UserMessage.message is blank:false and notify() does `message ?: ''`,
            // so an absent body would fail validation — fall back to the subject.
            String body = p.message?.toString()?.trim() ?: subject
            // find(), not valueOf() — valueOf throws on an unknown level.
            MessageLevel level = MessageLevel.values()
                    .find { it.name() == p.level?.toString()?.toUpperCase() } ?: MessageLevel.INFO
            // Namespace the dedup key by source so devices can't collide.
            String dedupKey = p.dedupKey ? "mqtt.${source}.${p.dedupKey}" : null
            int cooldown = Math.min(1440, Math.max(1,
                    (p.cooldown ?: NotificationService.DEFAULT_COOLDOWN_MINUTES) as int))

            notificationService.notifyAdmins(level, subject, body, source, dedupKey, cooldown)
        } catch (Exception ex) {
            log.error("Failed to handle MQTT notification: ${ex.message}", ex)
        }
    }
}
