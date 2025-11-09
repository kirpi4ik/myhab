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

    /**
     * @deprecated Use 'id' instead. This field is kept for backward compatibility only.
     * Will be removed in a future version.
     */
    @Deprecated
    String uid = DomainUtil.NULL_UID
    Date tsCreated = DomainUtil.NULL_DATE
    Date tsUpdated = DomainUtil.NULL_DATE
    EntityType entityType = EntityType.get(this)

    EntityType getEntityType() {
        return entityType
    }

    /**
     * Backward-compatible getter for uid that returns id.toString()
     * @deprecated Use 'id' instead
     */
    @Deprecated
    String getUid() {
        if (id) {
            return id.toString()
        }
        return uid ?: DomainUtil.NULL_UID
    }

    void beforeInsert() {
        if (this.tsCreated == DomainUtil.NULL_DATE) {
            def now = DateTime.now().toDate()
            this.tsCreated = now
        }
        log.trace 'Before inserting [' + this.entityType + ']... ' + (id ?: 'new')
    }

    void beforeUpdate() {
        def now = DateTime.now().toDate()
        this.tsUpdated = now
        log.trace 'Before updating [' + this.entityType + ']... ' + (id ?: 'new')
    }
//  static mapping = {
//    table '`device_event_logs`'
//    version true
//  }

    static constraints = {
        uid column: "uid", nullable: true
        tsCreated column: "ts_created", nullable: true
        tsUpdated column: "ts_updated", nullable: true
        entityType column: "en_type", nullable: true
    }

}
