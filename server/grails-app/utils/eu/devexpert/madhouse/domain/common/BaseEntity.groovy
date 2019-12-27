package eu.devexpert.madhouse.domain.common

import eu.devexpert.madhouse.domain.EntityType
import eu.devexpert.madhouse.domain.DomainUtil
import org.joda.time.DateTime

/**
 *
 */
abstract class BaseEntity implements Serializable {
  String uid = DomainUtil.NULL_UID
  Date tsCreated = DomainUtil.NULL_DATE
  Date tsUpdated = DomainUtil.NULL_DATE
  EntityType entityType

  def beforeInsert() {
    if (uid == DomainUtil.NULL_UID) {
      uid = UUID.randomUUID().toString()
    }
    if (tsCreated == DomainUtil.NULL_DATE) {
      def date = DateTime.now().toDate()
      tsCreated = date
      if (tsUpdated == DomainUtil.NULL_DATE) {
        tsUpdated = date
      }
    }
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
