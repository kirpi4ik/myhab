package eu.devexpert.madhouse.domain

import eu.devexpert.madhouse.domain.common.BaseEntity
import eu.devexpert.madhouse.domain.device.Cable
import eu.devexpert.madhouse.domain.device.Device
import eu.devexpert.madhouse.domain.device.DevicePeripheral
import eu.devexpert.madhouse.domain.device.PatchPanel
import eu.devexpert.madhouse.domain.device.PeripheralCategory
import eu.devexpert.madhouse.domain.device.port.DevicePort
import eu.devexpert.madhouse.domain.device.port.PortValue
import eu.devexpert.madhouse.domain.infra.Zone
import eu.devexpert.madhouse.domain.job.EventData

/**
 *
 */
enum EntityType {
    CABLE(Cable.class),
    DEVICE(Device.class),
    PERIPHERAL(DevicePeripheral.class),
    PERIPHERAL_CATEGORY(PeripheralCategory.class),
    ZONE(Zone.class),
    EVENT_DATA(EventData.class),
    PATCH_PANEL(PatchPanel.class),
    PORT(DevicePort.class),
    PORT_VALUE(PortValue.class);

    static <T extends BaseEntity> EntityType get(T type) {
        return values().find {
            it.typeClass == type.class
        }
    }

    static <T extends BaseEntity> EntityType get(Class<T> typeClass) {
        return values().find {
            it.typeClass == typeClass
        }
    }

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
