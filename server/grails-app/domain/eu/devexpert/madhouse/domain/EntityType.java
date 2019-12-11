package eu.devexpert.madhouse.domain;

import eu.devexpert.madhouse.domain.device.Cable;
import eu.devexpert.madhouse.domain.device.Device;
import eu.devexpert.madhouse.domain.device.DevicePeripheral;
import eu.devexpert.madhouse.domain.device.PatchPanel;
import eu.devexpert.madhouse.domain.device.port.DevicePort;
import eu.devexpert.madhouse.domain.device.port.PortValue;
import eu.devexpert.madhouse.domain.common.BaseEntity;
import eu.devexpert.madhouse.domain.infra.Zone;
import eu.devexpert.madhouse.domain.job.EventData;

/**
 *
 */
public enum EntityType {
    CABLE(Cable.class),
    DEVICE(Device.class),
    PERIPHERAL(DevicePeripheral.class),
    ZONE(Zone.class),
    EVENT_DATA(EventData.class),
    PATCH_PANEL(PatchPanel.class),
    PORT(DevicePort.class),
    PORT_VALUE(PortValue.class);

    private final Class<? extends BaseEntity> typeClass;

    EntityType(Class<? extends BaseEntity> typeClass) {
        this.typeClass = typeClass;
    }

//    <T extends BaseEntity> Class<T> getClas() {
//        return (Class<T>)typeClass;
//    }

    public boolean isEqual(String name) {
        return this.name().equalsIgnoreCase(name);
    }
}
