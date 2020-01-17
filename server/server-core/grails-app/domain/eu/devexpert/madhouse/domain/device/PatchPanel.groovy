package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class PatchPanel extends BaseEntity {
    Rack rack
    String code
    String description
    Set<Cable> cables
    static belongsTo = [Rack]
    static hasMany = [cables: Cable]

    static constraints = {
    }
    static mapping = {
        table '`rack_patch_panels`'
    }
    static graphql = true
}