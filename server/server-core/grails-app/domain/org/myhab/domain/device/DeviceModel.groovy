package org.myhab.domain.device

/**
 * Supported models
 */
enum DeviceModel {
    MEGAD_2561_RTC,
    ESP32,
    TMEZON_INTERCOM,
    NIBE_F1145_8_EM,
    ELECTRIC_METER_DTS,
    CAM_ONVIF,
    HUAWEI_SUN2000_12KTL_M2,
    HUAWEI_DONGLE,
    OPEN_METEO_API,
    /**
     * Segway Navimow robotic lawn mower. One value covers the whole H / i
     * series — the cloud REST API is uniform across SKUs, so we only need
     * separate enum members if a future feature actually diverges per model.
     */
    NAVIMOW_SEGWAY
}