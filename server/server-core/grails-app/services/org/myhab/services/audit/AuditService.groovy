package org.myhab.services.audit

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.myhab.domain.EntityType
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.events.AuditSource
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData

/**
 * Single, authoritative writer of audit-log rows ({@code EventData} → table
 * {@code event_log}). Every port/peripheral state change funnels through here so
 * the audit trail is consistent (one key space, structured source + actor) and
 * reliable (guaranteed, synchronous write — not the fire-and-forget
 * {@code publish('evt_log')} bus that silently dropped failures).
 *
 * <p>Field convention:
 * {@code p0}=topic, {@code p1}=EntityType, {@code p2}=entity id, {@code p3}=
 * {@link AuditSource}, {@code p4}=action/value, {@code p5}=JSON details
 * (cross-references, status, error), {@code p6}=actor (username / CRON / EVENT /
 * SYSTEM).</p>
 *
 * <p>Writes use {@code withNewTransaction} so the row commits independently of
 * the caller's transaction (notably {@code PowerService} is
 * {@code @Transactional(readOnly = true)}), mirroring the existing
 * {@code Configuration} upsert pattern there.</p>
 */
@Slf4j
class AuditService {

    /**
     * Record a port/peripheral state change (ON/OFF/TOGGLE).
     *
     * @param entityType PORT or PERIPHERAL
     * @param entityId   numeric entity id (never a device code)
     * @param action     the PortAction performed
     * @param source     structured origin
     * @param actor      initiating user, or CRON/EVENT/SYSTEM when unattended
     * @param details    optional cross-references / status (e.g. [peripheralId: 5, status: 'FAILED'])
     * @return the persisted EventData
     */
    EventData logStateChange(EntityType entityType, Long entityId, PortAction action,
                             AuditSource source, String actor, Map details = [:]) {
        return persist(TopicName.EVT_LOG.id(), entityType, entityId, action?.toString(), source, actor, details)
    }

    /**
     * Record a non-PortAction audit event (door unlock, gate open, …).
     */
    EventData log(String topic, EntityType entityType, Long entityId, String value,
                  AuditSource source, String actor, Map details = [:]) {
        return persist(topic, entityType, entityId, value, source, actor, details)
    }

    /**
     * Persist a pre-built {@code EventData}. Used by the legacy {@code evt_log}
     * subscriber shim so that rows still funnel through this single writer while
     * remaining publishers are migrated. Same guaranteed, own-transaction write.
     */
    EventData save(EventData ev) {
        EventData.withNewTransaction {
            ev.save(failOnError: true, flush: true)
        }
        return ev
    }

    private EventData persist(String topic, EntityType entityType, Long entityId, String value,
                              AuditSource source, String actor, Map details) {
        // `actionId` is a first-class correlation column, not a p5 detail — pull it
        // out so command + device-confirmation rows can be grouped by it.
        String actionId = details?.actionId as String
        def cleaned = details?.findAll { it.key != 'actionId' && it.value != null }
        EventData ev = new EventData(
                p0: topic,
                p1: entityType?.name(),
                p2: entityId != null ? entityId.toString() : null,
                p3: (source ?: AuditSource.SYSTEM).name(),
                p4: value,
                p5: (cleaned ? JsonOutput.toJson(cleaned) : null),
                p6: actor ?: 'SYSTEM',
                actionId: actionId
        )
        // Own transaction: guaranteed commit, independent of the (possibly
        // read-only) caller transaction. Let exceptions propagate — a failed
        // audit write is a real incident, not something to swallow.
        EventData.withNewTransaction {
            ev.save(failOnError: true, flush: true)
        }
        log.debug("Audit: ${entityType}#${entityId} ${value} source=${ev.p3} actor=${ev.p6}")
        return ev
    }
}
