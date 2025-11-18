package org.myhab.domain.common

import org.myhab.domain.DomainUtil
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.myhab.domain.EntityType
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData

/**
 *
 */
@Slf4j
abstract class BaseEntity implements Serializable {

//    @Id
//    @Column(columnDefinition = "BINARY(16)")
//    @GeneratedValue(generator = "uuid2")
//    @GenericGenerator(name = "uuid2", strategy = "uuid2")
//    private UUID id;

    Date tsCreated = DomainUtil.NULL_DATE
    Date tsUpdated = DomainUtil.NULL_DATE
    EntityType entityType = EntityType.get(this)

    EntityType getEntityType() {
        return entityType
    }

    void beforeInsert() {
        if (this.tsCreated == DomainUtil.NULL_DATE) {
            // Store timestamps in UTC
            def now = DateTime.now(org.joda.time.DateTimeZone.UTC).toDate()
            this.tsCreated = now
        }
        // Also set ts_updated on insert for tables that never get updated
        if (this.tsUpdated == DomainUtil.NULL_DATE) {
            this.tsUpdated = this.tsCreated
        }
        log.trace 'Before inserting [' + this.entityType + ']... ' + (id ?: 'new')
    }

    void beforeUpdate() {
        // Store timestamps in UTC
        def now = DateTime.now(org.joda.time.DateTimeZone.UTC).toDate()
        this.tsUpdated = now
        log.trace 'Before updating [' + this.entityType + ']... ' + (id ?: 'new')
    }
//  static mapping = {
//    table '`device_event_logs`'
//    version true
//  }

    static constraints = {
        tsCreated column: "ts_created", nullable: true
        tsUpdated column: "ts_updated", nullable: true
        entityType column: "en_type", nullable: true
    }

}
