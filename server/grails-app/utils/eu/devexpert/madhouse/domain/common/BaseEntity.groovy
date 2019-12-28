package eu.devexpert.madhouse.domain.common

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.DomainUtil
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.engine.event.PostInsertEvent
import org.grails.datastore.mapping.engine.event.PostUpdateEvent
import org.joda.time.DateTime

/**
 *
 */
@Slf4j
abstract class BaseEntity implements Serializable {
    String uid = DomainUtil.NULL_UID
    Date tsCreated = DomainUtil.NULL_DATE
    Date tsUpdated = DomainUtil.NULL_DATE
    EntityType entityType

    void beforeInsert() {
        if (this.uid == DomainUtil.NULL_UID) {
            this.uid = UUID.randomUUID().toString()
        }
        if (this.tsCreated == DomainUtil.NULL_DATE) {
            def now = DateTime.now().toDate()
            this.tsCreated = now
        }
        log.info 'Before inserting ... ' + this.uid
    }

    void beforeUpdate() {
        def now = DateTime.now().toDate()
        this.tsUpdated = now
        log.info 'Before updating ... ' + this.uid
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
