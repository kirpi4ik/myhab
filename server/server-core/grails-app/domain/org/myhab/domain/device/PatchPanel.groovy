package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

class PatchPanel extends BaseEntity {
    Rack rack
    String name
    String description
    int size
    Set<Cable> cables
    static belongsTo = [rack: Rack]
    static hasMany = [cables: Cable]

    static constraints = {
        size nullable: true
    }
    static mapping = {
        table '`rack_patch_panels`'
    }
    static graphql = true
}