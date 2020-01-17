package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class DeviceType extends BaseEntity {
    String name
    static mapping = {
        table '`device_types`'
    }
    static graphql = true
}
