package eu.devexpert.madhouse.domain

import eu.devexpert.madhouse.domain.common.BaseEntity

/**
 * The key has by convention following tree structure:
 * <entityType>.<uid>.key
 */
class EntityConfiguration extends BaseEntity {
  String cfKey
  String cfEntityUid
  String cfEntityType
  String value
  String description

  static constraints = {
  }
  static mapping = {
    table '`entity_configurations`'
//    cfKey index: 'kf_key_idx'
//    cfEntityUid index: 'cf_ent_uid_idx'
//    cfEntityType index: 'cf_ent_type_idx'
  }
    static graphql = true
}
