package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class CableCategory extends BaseEntity {
    String name
    Set<Cable> cables;
    static hasMany = [cables: Cable]
    static mapping = {
        table '`cable_categories`'
        sort name: "asc"
    }
    static graphql = true

}
