package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

class PeripheralCategory extends BaseEntity {

    String title
    String name
    /** Default MDI icon for dashboard-screen markers (e.g. 'mdi-lightbulb'); per-widget overrides win. */
    String icon
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
        icon nullable: true
    }
}
