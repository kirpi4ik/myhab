package org.myhab

/**
 *
 */
interface ConfigKey {
    public static final String STATE_ON_TIMEOUT = "key.on.timeout"
    /**
     * One-shot in-memory-style timeout override (seconds) written by
     * PowerService.execute when a DSL caller passes `timeout:` to switchOn,
     * and consumed (then deleted) by PortValueService.updateExpirationTime
     * when the device confirms ON. Wins over STATE_ON_TIMEOUT for one cycle.
     * Stored as a Configuration row so it shows up in the UI and is traceable.
     */
    public static final String STATE_ON_TIMEOUT_OVERRIDE = "key.on.timeout.override"
    public static final String CONFIG_LIST_RELATED_PERIPHERAL_IDS = "key.peripheral.list.relPer.ids"
    public static final String CONFIG_LIST_SCHEDULED_TEMP = "key.temp.schedule.list.value"
    public static final String CONFIG_TEMP_ALL_DAY = "key.temp.allDay.value"
    public static final String CONFIG_LIGHT_RGB_RANDOM = "key.light.rgbRandom"
    public static final String CONFIG_LIGHT_RGB_COLOR = "key.light.rgb.color"
}
