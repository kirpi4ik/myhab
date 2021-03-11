package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

class PeripheralCategory extends BaseEntity {

    String title
    String name
    Set<DevicePeripheral> peripherals;
    Set<Cable> cables;
    static hasMany = [peripherals: DevicePeripheral, cables: Cable]
    static mapping = {
        table '`device_peripherals_categories`'
        sort name: "asc"
    }
    static graphql = true
    static constraints = {
        title nullable: true
    }
}
