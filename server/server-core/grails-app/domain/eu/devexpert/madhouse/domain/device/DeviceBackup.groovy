package eu.devexpert.madhouse.domain.device

import eu.devexpert.madhouse.domain.common.BaseEntity

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
        columns {
            firmware type: 'blob'
        }
    }
}
