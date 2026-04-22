package org.myhab.domain.device

import org.myhab.domain.common.BaseEntity

class DeviceBackup extends BaseEntity {
    String frmVersion
    byte[] firmware
    String configuration
    Device device

    static belongsTo = [device: Device]
    static constraints = {
        firmware nullable: true
        configuration nullable: true
    }
    static mapping = {
        firmware type: 'blob'
        configuration type: 'text', sqlType: 'text'
    }
}
