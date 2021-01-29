package eu.devexpert.madhouse.domain.common

import eu.devexpert.madhouse.domain.DomainUtil
import eu.devexpert.madhouse.domain.EntityType
import groovy.util.logging.Slf4j
import org.hibernate.annotations.GenericGenerator
import org.joda.time.DateTime

import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.Id

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

    String uid = DomainUtil.NULL_UID
    Date tsCreated = DomainUtil.NULL_DATE
    Date tsUpdated = DomainUtil.NULL_DATE
    EntityType entityType = EntityType.get(this)

    EntityType getEntityType() {
        return entityType
    }

    void beforeInsert() {
        if (this.uid == DomainUtil.NULL_UID) {
            this.uid = UUID.randomUUID().toString()
        }
        if (this.tsCreated == DomainUtil.NULL_DATE) {
            def now = DateTime.now().toDate()
            this.tsCreated = now
        }
        log.trace 'Before inserting ... ' + this.uid
    }

    void beforeUpdate() {
        def now = DateTime.now().toDate()
        this.tsUpdated = now
        log.trace 'Before updating ... ' + this.uid
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
