package org.myhab.domain

import org.myhab.domain.common.BaseEntity
import org.myhab.domain.device.Device
import org.myhab.domain.device.Cable
import org.myhab.domain.device.CableCategory
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.PatchPanel
import org.myhab.domain.device.PeripheralCategory
import org.myhab.domain.device.Rack
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortValue
import org.myhab.domain.infra.Zone
import org.myhab.domain.job.EventData
import org.myhab.domain.device.Scenario
import org.myhab.domain.job.Job
import org.myhab.domain.infra.Layer
import org.myhab.domain.job.JobTag

/**
 *
 */
enum EntityType {
    CABLE(Cable.class),
    CABLE_CATEGORY(CableCategory.class),
    DEVICE(Device.class),
    PERIPHERAL(DevicePeripheral.class),
    PERIPHERAL_CATEGORY(PeripheralCategory.class),
    ZONE(Zone.class),
    RACK(Rack.class),
    EVENT_DATA(EventData.class),
    PATCH_PANEL(PatchPanel.class),
    PORT(DevicePort.class),
    CONFIG(Configuration.class),
    PORT_VALUE(PortValue.class),
    TS_STATISTIC(TimeSeriesStatistic.class),
    SCENARIO(Scenario.class),
    JOB(Job.class),
    JOB_TAG(JobTag.class),
    LAYER(Layer.class),
    USER(User.class);

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

    public static EntityType byName(def name) {
        return values().find { it ->
            it.name().equalsIgnoreCase(name)
        }
    }
}
