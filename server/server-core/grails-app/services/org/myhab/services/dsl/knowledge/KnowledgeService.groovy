package org.myhab.services.dsl.knowledge

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.infra.Zone

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Read-only knowledge predicates exposed to the scenario DSL.
 *
 * <p>Bound into the scenario DSL via
 * {@link org.myhab.services.dsl.CompositeDelegate} configured by
 * {@link org.myhab.services.dsl.DslService}, so methods are callable directly
 * (no namespace prefix) from a scenario:</p>
 *
 * <pre>
 * if (isRaining()) switchOff([peripheralIds: [42]])
 * if (isNight() &amp;&amp; currentExternTemperature() &lt; 5) switchOn([peripheralIds: [11]])
 * </pre>
 *
 * <p>Configuration (read from {@code ConfigProvider} — GIT-backed yaml,
 * editable via the Application Configuration UI):</p>
 * <ul>
 *   <li>{@code knowledge.meteo.device.id} (Long) — Meteo station Device id.</li>
 *   <li>{@code knowledge.zone.exterior.id} (Long) — Exterior Zone id.</li>
 *   <li>{@code knowledge.rain.peripheral.id} (Long, optional) — dedicated rain-sensor peripheral id.</li>
 *   <li>{@code knowledge.rain.precipitation.threshold} (Double, default 0.0) —
 *       meteo {@code current.precipitation} &gt; threshold ⇒ raining.</li>
 * </ul>
 *
 * <p>All methods degrade gracefully when configuration or data is missing
 * (return {@code false} / {@code null} and log a warning) so a partially
 * configured installation does not break scenario execution.</p>
 */
@Slf4j
@Transactional(readOnly = true)
class KnowledgeService {

    static final String CFG_METEO_DEVICE_ID = 'knowledge.meteo.device.id'
    static final String CFG_EXTERIOR_ZONE_ID = 'knowledge.zone.exterior.id'
    static final String CFG_RAIN_PERIPHERAL_ID = 'knowledge.rain.peripheral.id'
    static final String CFG_RAIN_THRESHOLD = 'knowledge.rain.precipitation.threshold'

    static final String METEO_PORT_PRECIPITATION = 'current.precipitation'
    static final String METEO_PORT_IS_DAY = 'current.is_day'
    static final String METEO_PORT_TEMPERATURE = 'current.temperature_2m'
    static final String METEO_PORT_TIMEZONE = 'timezone'
    static final String METEO_PORT_SUNRISE = 'daily.sunrise'
    static final String METEO_PORT_SUNSET = 'daily.sunset'

    static final String TEMP_CATEGORY = 'TEMP'

    def configProvider

    /**
     * Is it currently raining?
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If {@code knowledge.rain.peripheral.id} resolves to a peripheral with at least
     *       one connected port, use that port's value. Truthy when value equals
     *       {@code PortAction.ON} or parses as a positive number.</li>
     *   <li>Otherwise fall back to meteo: {@code current.precipitation &gt; threshold}
     *       (threshold from {@code knowledge.rain.precipitation.threshold}, default 0.0).</li>
     * </ol>
     *
     * @return {@code true} if any source indicates rain; {@code false} otherwise
     *         (including when no source is available).
     */
    boolean isRaining() {
        DevicePeripheral peripheral = rainPeripheral()
        if (peripheral != null) {
            def port = peripheral.connectedTo?.find { it != null }
            String value = port?.value
            Boolean fromPeripheral = interpretBooleanValue(value)
            if (fromPeripheral != null) {
                log.debug("isRaining: peripheral ${peripheral.id} value='${value}' -> ${fromPeripheral}")
                return fromPeripheral
            }
            log.warn("isRaining: peripheral ${peripheral.id} has no usable value; falling back to meteo")
        }

        Double threshold = readDoubleConfig(CFG_RAIN_THRESHOLD, 0.0d)
        Double precipitation = parseDouble(meteoPortValue(METEO_PORT_PRECIPITATION))
        if (precipitation == null) {
            log.warn("isRaining: meteo precipitation unavailable; returning false")
            return false
        }
        boolean result = precipitation > threshold
        log.debug("isRaining: meteo precipitation=${precipitation} threshold=${threshold} -> ${result}")
        return result
    }

    /**
     * Is the sun currently up?
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>Meteo {@code current.is_day} flag (Open-Meteo, "1"/"0").</li>
     *   <li>Compare now to today's {@code daily.sunrise[0]} / {@code daily.sunset[0]}
     *       in the meteo's configured timezone.</li>
     *   <li>Clock-based heuristic: {@code 06:00 <= now < 20:00} in the JVM
     *       default timezone (logged at warn).</li>
     * </ol>
     */
    boolean isDay() {
        String isDayValue = meteoPortValue(METEO_PORT_IS_DAY)
        if (isDayValue != null) {
            String trimmed = isDayValue.trim()
            if (trimmed == '1' || trimmed.equalsIgnoreCase('true')) return true
            if (trimmed == '0' || trimmed.equalsIgnoreCase('false')) return false
        }

        Boolean fromSunWindow = isDayFromSunWindow()
        if (fromSunWindow != null) {
            return fromSunWindow
        }

        log.warn("isDay: meteo data unavailable; using clock-based fallback (06:00-20:00)")
        LocalTime now = LocalTime.now()
        return !now.isBefore(LocalTime.of(6, 0)) && now.isBefore(LocalTime.of(20, 0))
    }

    /** Inverse of {@link #isDay()}. */
    boolean isNight() {
        return !isDay()
    }

    /**
     * Current outside temperature, rounded to the nearest integer.
     *
     * <p>Averages the values of all {@code TEMP} peripherals attached to the
     * exterior zone (skipping null / unparseable readings). When the zone has
     * no usable readings, falls back to meteo {@code current.temperature_2m}.</p>
     *
     * @return rounded outside temperature in °C, or {@code null} when no
     *         source is available.
     */
    Integer currentExternTemperature() {
        Zone zone = exteriorZone()
        if (zone != null) {
            List<Double> readings = []
            zone.peripherals?.findAll { it?.category?.name == TEMP_CATEGORY }?.each { peripheral ->
                peripheral.connectedTo?.each { port ->
                    Double parsed = parseDouble(port?.value)
                    if (parsed != null) readings << parsed
                }
            }
            if (!readings.isEmpty()) {
                double avg = readings.sum() / readings.size()
                int rounded = (int) Math.round(avg)
                log.debug("currentExternTemperature: zone ${zone.id} readings=${readings} avg=${avg} -> ${rounded}")
                return rounded
            }
            log.debug("currentExternTemperature: zone ${zone.id} has no usable TEMP readings; falling back to meteo")
        }

        Double meteoTemp = parseDouble(meteoPortValue(METEO_PORT_TEMPERATURE))
        if (meteoTemp == null) {
            log.warn("currentExternTemperature: no zone reading and no meteo temperature available")
            return null
        }
        return (int) Math.round(meteoTemp)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Device meteoDevice() {
        Long id = readLongConfig(CFG_METEO_DEVICE_ID)
        if (id == null) {
            log.warn("Knowledge config '${CFG_METEO_DEVICE_ID}' is not set")
            return null
        }
        Device device = Device.get(id)
        if (device == null) {
            log.warn("Meteo device id=${id} (from ${CFG_METEO_DEVICE_ID}) not found")
        }
        return device
    }

    private Zone exteriorZone() {
        Long id = readLongConfig(CFG_EXTERIOR_ZONE_ID)
        if (id == null) {
            log.warn("Knowledge config '${CFG_EXTERIOR_ZONE_ID}' is not set")
            return null
        }
        Zone zone = Zone.get(id)
        if (zone == null) {
            log.warn("Exterior zone id=${id} (from ${CFG_EXTERIOR_ZONE_ID}) not found")
        }
        return zone
    }

    private DevicePeripheral rainPeripheral() {
        Long id = readLongConfig(CFG_RAIN_PERIPHERAL_ID)
        if (id == null) return null
        DevicePeripheral peripheral = DevicePeripheral.get(id)
        if (peripheral == null) {
            log.warn("Rain peripheral id=${id} (from ${CFG_RAIN_PERIPHERAL_ID}) not found")
        }
        return peripheral
    }

    private String meteoPortValue(String internalRef) {
        Device device = meteoDevice()
        if (device == null) return null
        def port = device.ports?.find { it?.internalRef == internalRef }
        return port?.value
    }

    private List meteoPortArray(String internalRef) {
        String raw = meteoPortValue(internalRef)
        if (raw == null) return []
        if (raw instanceof List) return raw as List
        try {
            def parsed = new JsonSlurper().parseText(raw.toString())
            return parsed instanceof List ? (parsed as List) : []
        } catch (Exception ex) {
            log.warn("Failed to parse meteo port '${internalRef}' as JSON array: ${ex.message}")
            return []
        }
    }

    /**
     * Compare now to today's sunrise/sunset window. Returns {@code null} when
     * the meteo data isn't available.
     */
    private Boolean isDayFromSunWindow() {
        List sunrises = meteoPortArray(METEO_PORT_SUNRISE)
        List sunsets = meteoPortArray(METEO_PORT_SUNSET)
        if (sunrises.isEmpty() || sunsets.isEmpty()) return null

        ZoneId zone = resolveMeteoZone()
        try {
            LocalDateTime sunrise = LocalDateTime.parse(sunrises[0].toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            LocalDateTime sunset = LocalDateTime.parse(sunsets[0].toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            LocalDateTime now = ZonedDateTime.now(zone).toLocalDateTime()
            return !now.isBefore(sunrise) && now.isBefore(sunset)
        } catch (Exception ex) {
            log.warn("Failed to evaluate sunrise/sunset window: ${ex.message}")
            return null
        }
    }

    private ZoneId resolveMeteoZone() {
        String tz = meteoPortValue(METEO_PORT_TIMEZONE)
        if (tz) {
            try {
                return ZoneId.of(tz.trim())
            } catch (Exception ignored) {
                log.debug("Unknown meteo timezone '${tz}', falling back to system default")
            }
        }
        return ZoneId.systemDefault()
    }

    private Long readLongConfig(String key) {
        try {
            def value = configProvider?.get(Long.class, key)
            return value as Long
        } catch (Exception ex) {
            log.warn("Failed to read config '${key}' as Long: ${ex.message}")
            return null
        }
    }

    private Double readDoubleConfig(String key, Double defaultValue) {
        try {
            def value = configProvider?.get(Double.class, key)
            return value != null ? (value as Double) : defaultValue
        } catch (Exception ex) {
            log.warn("Failed to read config '${key}' as Double: ${ex.message}; using default ${defaultValue}")
            return defaultValue
        }
    }

    private static Double parseDouble(Object raw) {
        if (raw == null) return null
        try {
            double d = Double.parseDouble(raw.toString().trim())
            return Double.isNaN(d) ? null : d
        } catch (NumberFormatException ignored) {
            return null
        }
    }

    /**
     * Interpret a port value as a boolean rain indicator.
     * @return {@code true} / {@code false} when the value is a recognized state,
     *         {@code null} when the value is missing or unparseable so the caller
     *         can fall through to the next source.
     */
    private static Boolean interpretBooleanValue(String value) {
        if (value == null) return null
        String trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.equalsIgnoreCase(PortAction.ON.name()) || trimmed.equalsIgnoreCase('TRUE') || trimmed == '1') return true
        if (trimmed.equalsIgnoreCase(PortAction.OFF.name()) || trimmed.equalsIgnoreCase('FALSE') || trimmed == '0') return false
        Double numeric = parseDouble(trimmed)
        if (numeric != null) return numeric > 0.0d
        return null
    }
}
