package org.myhab.services.dsl.knowledge

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.myhab.domain.device.Device
import org.myhab.domain.device.DevicePeripheral
import org.myhab.domain.device.port.PortAction
import org.myhab.domain.infra.Zone

import java.time.Duration
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
    static final String METEO_PORT_HOURLY_PRECIPITATION = 'hourly.precipitation'
    static final String METEO_PORT_HOURLY_TIME = 'hourly.time'

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
     * How many minutes did it rain within the last {@code lookbackMinutes}?
     *
     * <p>Counts, at hourly resolution, the minutes of the recent window during
     * which precipitation exceeded {@code knowledge.rain.precipitation.threshold}.
     * The rain need not be continuous. Based on the meteo {@code hourly.precipitation}
     * / {@code hourly.time} arrays (Open-Meteo). Future hours are excluded.</p>
     *
     * <p>Typical use — skip a watering scenario when it already rained enough:</p>
     * <pre>if (rainDuration(120) > 60) skip()  // > 60 min of rain in the last 2h</pre>
     *
     * @param lookbackMinutes size of the recent window, in minutes
     * @return minutes of rain within the window; {@code 0} when data is unavailable
     */
    int rainDuration(int lookbackMinutes) {
        Double threshold = readDoubleConfig(CFG_RAIN_THRESHOLD, 0.0d)
        long minutes = 0L
        rainSlotsInWindow(lookbackMinutes).each { slot ->
            if (slot.precipitation > threshold) {
                minutes += (slot.overlapMinutes as long)
            }
        }
        log.debug("rainDuration: lookback=${lookbackMinutes}min -> ${minutes}min")
        return (int) minutes
    }

    /**
     * How much rain (mm) fell within the last {@code lookbackMinutes}?
     *
     * <p>Sums the meteo {@code hourly.precipitation} values overlapping the recent
     * window, prorated by each hour's overlap fraction. Future hours are excluded.</p>
     *
     * <pre>if (rainAmount(120) > 5.0) skip()  // > 5 mm of rain in the last 2h</pre>
     *
     * @param lookbackMinutes size of the recent window, in minutes
     * @return millimetres of rain within the window, rounded to 1 decimal;
     *         {@code 0.0} when data is unavailable
     */
    double rainAmount(int lookbackMinutes) {
        double mm = 0.0d
        rainSlotsInWindow(lookbackMinutes).each { slot ->
            mm += (slot.precipitation as double) * ((slot.overlapMinutes as double) / 60.0d)
        }
        double rounded = Math.round(mm * 10.0d) / 10.0d
        log.debug("rainAmount: lookback=${lookbackMinutes}min -> ${rounded}mm")
        return rounded
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
     * Hourly precipitation slots overlapping the recent window {@code (now - lookback, now]}.
     *
     * <p>Reads the meteo {@code hourly.precipitation} / {@code hourly.time} arrays and,
     * for each hourly slot, computes how many of its minutes fall inside the window.
     * Open-Meteo precipitation is a preceding-hour sum, so slot {@code i} covers the
     * interval {@code (time[i] - 1h, time[i]]}. Future portions (beyond {@code now})
     * are clamped out.</p>
     *
     * @return list of {@code [overlapMinutes: long, precipitation: double]} maps for
     *         slots with positive overlap; empty when input is invalid/unavailable.
     */
    private List<Map> rainSlotsInWindow(int lookbackMinutes) {
        if (lookbackMinutes <= 0) {
            log.warn("rainSlotsInWindow: non-positive lookbackMinutes=${lookbackMinutes}; returning empty")
            return []
        }
        List times = meteoPortArray(METEO_PORT_HOURLY_TIME)
        List precipitations = meteoPortArray(METEO_PORT_HOURLY_PRECIPITATION)
        if (times.isEmpty() || precipitations.isEmpty()) {
            log.warn("rainSlotsInWindow: meteo hourly precipitation/time unavailable; returning empty")
            return []
        }

        ZoneId zone = resolveMeteoZone()
        LocalDateTime now = ZonedDateTime.now(zone).toLocalDateTime()
        LocalDateTime windowStart = now.minusMinutes(lookbackMinutes)

        List<Map> slots = []
        int count = Math.min(times.size(), precipitations.size())
        for (int i = 0; i < count; i++) {
            Double precipitation = parseDouble(precipitations[i])
            if (precipitation == null) continue
            LocalDateTime slotEnd
            try {
                slotEnd = LocalDateTime.parse(times[i].toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (Exception ex) {
                log.warn("rainSlotsInWindow: failed to parse hourly time '${times[i]}': ${ex.message}")
                continue
            }
            LocalDateTime slotStart = slotEnd.minusHours(1)
            LocalDateTime overlapStart = slotStart.isAfter(windowStart) ? slotStart : windowStart
            LocalDateTime overlapEnd = slotEnd.isBefore(now) ? slotEnd : now
            if (overlapEnd.isAfter(overlapStart)) {
                long overlapMinutes = Duration.between(overlapStart, overlapEnd).toMinutes()
                if (overlapMinutes > 0) {
                    slots << [overlapMinutes: overlapMinutes, precipitation: precipitation]
                }
            }
        }
        return slots
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
