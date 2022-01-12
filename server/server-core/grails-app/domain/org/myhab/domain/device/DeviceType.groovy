package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

class DeviceType extends BaseEntity {
    String name
    static mapping = {
        table '`device_types`'
    }
    static graphql = true
}
