package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

class DeviceCategory extends BaseEntity {
    String name
    static mapping = {
        table '`device_categories`'
    }
    static graphql = true
}
