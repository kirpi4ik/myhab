package org.myhab.jobs

import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.MessageLevel
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortState
import org.myhab.domain.device.port.PortType
import org.myhab.services.NotificationService
import org.myhab.services.navimow.NavimowApiClient
import org.myhab.services.navimow.NavimowApiException
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

/**
 * Periodic poll of Segway's cloud REST API for every Navimow registered in
 * the DB. Mirrors the pattern used by {@link NibeInfoSyncJob} and
 * {@link HuaweiInfoSyncJob}:
 *
 * <ol>
 *   <li>Check the {@code quartz.jobs.navimowInfoSync.enabled} runtime flag.</li>
 *   <li>For each {@link DeviceModel#NAVIMOW_SEGWAY} device, read token /
 *       base URL / Segway device-id from per-device {@code Configuration} rows.</li>
 *   <li>Batch-fetch statuses via {@link NavimowApiClient#getStatuses}.</li>
 *   <li>Capture the previous values (from {@code DevicePort.value}), publish
 *       new values through {@code mqttTopicService.publishPortValue} so the
 *       normal persistence + UI broadcast pipeline runs.</li>
 *   <li>Diff old vs new and surface notable transitions via
 *       {@link NotificationService} as user-visible {@code UserMessage}s.</li>
 * </ol>
 *
 * <p>The job is OPT-IN at the application.yml level — it stays disabled by
 * default so an installation without a Navimow doesn't generate noisy "OFFLINE"
 * notifications. Enable {@code quartz.jobs.navimowInfoSync.enabled: true}
 * once the per-device Configuration rows are populated.</p>
 */
@Slf4j
@DisallowConcurrentExecution
@Transactional
class NavimowInfoSyncJob implements Job {

    // Per-port internal-ref namespace (e.g. "navimow.state", "navimow.battery").
    static final String PORT_PREFIX = 'navimow'

    // State names confirmed from segwaynavimow/NavimowHA const.py
    // (MOWER_STATUS_TO_ACTIVITY mapping): idle / mowing / paused / docked /
    // charging / returning / error / unknown. We match case-insensitively
    // against trimmed values just in case the server ever returns mixed case.
    static final Set<String> ACTIVE_STATES    = ['mowing'] as Set
    static final Set<String> RESTING_STATES   = ['idle', 'docked', 'charging', 'paused'] as Set
    static final Set<String> RETURNING_STATES = ['returning'] as Set

    // Raw-state → canonical mapping ported from segwaynavimow/navimow-sdk
    // mower_sdk/models.py:_RAW_STATE_TO_CANONICAL. Segway's REST API returns
    // values like "isDocked", "isRunning", "isPaused"; we collapse them to the
    // canonical lowercase set so the existing notification logic works.
    static final Map<String, String> RAW_STATE_TO_CANONICAL = [
            isDocked          : 'docked',
            isIdel            : 'idle',
            isIdle            : 'idle',
            isMapping         : 'mowing',
            isRunning         : 'mowing',
            isPaused          : 'paused',
            isDocking         : 'returning',
            Error             : 'error',
            error             : 'error',
            isLifted          : 'error',
            inSoftwareUpdate  : 'paused',
            'Self-Checking'   : 'idle',
            'Self-checking'   : 'idle',
            Offline           : 'unknown',
            offline           : 'unknown',
    ]

    static final int DEFAULT_LOW_BATTERY_THRESHOLD = 20

    MqttTopicService mqttTopicService
    NavimowApiClient navimowApiClient
    NotificationService notificationService
    def configProvider

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def cfg = Holders.grailsApplication?.config
        Boolean enabled = cfg?.getProperty('quartz.jobs.navimowInfoSync.enabled', Boolean)
        if (enabled == null) enabled = false
        if (!enabled) {
            log.trace("NavimowInfoSyncJob disabled — skipping")
            return
        }

        List<Device> mowers = Device.findAllByModel(DeviceModel.NAVIMOW_SEGWAY)
        if (mowers.isEmpty()) {
            log.trace("No NAVIMOW_SEGWAY devices registered — skipping")
            return
        }

        mowers.each { Device device ->
            try {
                syncOne(device)
            } catch (Exception ex) {
                log.error("NavimowInfoSyncJob failed for device ${device.code}: ${ex.message}", ex)
                safePublishStatus(device, DeviceStatus.OFFLINE)
            }
        }
    }

    private void syncOne(Device device) {
        String token = readConfig(device, CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_TOKEN.key())
        String baseUrl = readConfig(device, CfgKey.DEVICE.DEVICE_NAVIMOW_API_BASE_URL.key())
        String segwayId = readConfig(device, CfgKey.DEVICE.DEVICE_NAVIMOW_DEVICE_ID.key())
        if (!token || !baseUrl || !segwayId) {
            log.warn("NavimowInfoSyncJob: device ${device.code} is missing one of token/base_url/segway_device_id — marking OFFLINE")
            safePublishStatus(device, DeviceStatus.OFFLINE)
            return
        }

        Map<String, Map> statuses
        try {
            statuses = navimowApiClient.getStatuses(token, baseUrl, [segwayId])
        } catch (NavimowApiException ex) {
            log.error("NavimowInfoSyncJob: getStatuses failed for ${device.code}: ${ex.message}")
            // Surface a one-time WARN notification when an auth-related error is
            // the cause, so the user knows to re-run the "Connect Navimow account"
            // flow without having to read bootRun logs. Gated by the current
            // device.status — only fires on the ONLINE→OFFLINE transition, so
            // repeated 30s ticks while the token is dead don't spam the bell.
            if (device.status == DeviceStatus.ONLINE && isAuthFailure(ex)) {
                notifyTokenExpired(device, ex)
            }
            safePublishStatus(device, DeviceStatus.OFFLINE)
            return
        }

        Map status = statuses[segwayId]
        if (status == null) {
            log.warn("NavimowInfoSyncJob: Segway returned no entry for device id ${segwayId}")
            safePublishStatus(device, DeviceStatus.OFFLINE)
            return
        }
        // First-tick visibility: dump the raw payload so we can spot any field
        // we should be exposing. Use INFO for the first observation of each
        // session via a static flag.
        logFirstPayload(device.code, status)

        // Snapshot previous port values BEFORE publishing, so the notification
        // diff has both sides. Stored as a plain map keyed by internalRef.
        Map<String, String> previous = [:]
        device.ports?.each { DevicePort p ->
            if (p?.internalRef?.startsWith("${PORT_PREFIX}.")) {
                previous[p.internalRef] = p.value
            }
        }

        // Extract the canonical fields we care about from Segway's varied
        // payload shape (snake_case + camelCase + nested arrays). Each entry
        // maps a friendly port name to the extracted value, skipping nulls.
        Map<String, String> publishable = extractPublishableFields(status)
        publishable.each { String field, String value ->
            publishField(device, field, value)
        }

        // Emit notifications based on the diff (using the same normalised fields).
        emitNotifications(device, previous, publishable)

        if (device.status == DeviceStatus.OFFLINE) {
            safePublishStatus(device, DeviceStatus.ONLINE)
        }
    }

    /**
     * Boil Segway's heterogeneous response shape down to a flat map of
     * {@code internalRefSuffix → stringValue}. Skips fields with no usable
     * value so we don't auto-create empty ports.
     */
    private static Map<String, String> extractPublishableFields(Map status) {
        Map<String, String> out = [:]

        // State: vehicleState / state / status — any can be present, mapped via the SDK dict.
        String rawState = (status.vehicleState ?: status.state ?: status.status) as String
        String canonicalState = normaliseRawState(rawState)
        if (canonicalState) out.state = canonicalState
        if (rawState && rawState != canonicalState) out.rawState = rawState

        // Battery: prefer flat 'battery', fall back to capacityRemaining[].rawValue
        // with unit==PERCENTAGE (the actual REST shape).
        Integer battery = extractBattery(status)
        if (battery != null) out.battery = battery.toString()

        // Friendly battery label e.g. "Half", "Full" — Segway returns this sometimes.
        def friendlyBattery = status.descriptiveCapacityRemaining
        if (friendlyBattery != null) out.batteryLabel = friendlyBattery.toString()

        // Error fields: snake_case is what the REST API uses; camelCase is a
        // safe-also for any wrappers that already normalised it.
        def errorCode = status.error_code ?: status.errorCode
        if (errorCode != null && errorCode.toString().trim() && !'none'.equalsIgnoreCase(errorCode.toString())) {
            out.errorCode = errorCode.toString()
        }
        def errorMessage = status.error_message ?: status.errorMessage
        if (errorMessage != null && errorMessage.toString().trim()) out.errorMessage = errorMessage.toString()

        // Timing fields (seconds).
        def mowingTime = status.mowing_time ?: status.mowingTime
        if (mowingTime != null) out.mowingTime = mowingTime.toString()
        def totalMowingTime = status.total_mowing_time ?: status.totalMowingTime
        if (totalMowingTime != null) out.totalMowingTime = totalMowingTime.toString()

        // Signal strength + position (if present).
        if (status.signal_strength != null) out.signalStrength = status.signal_strength.toString()
        else if (status.signalStrength != null) out.signalStrength = status.signalStrength.toString()
        if (status.position instanceof Map) {
            Map pos = status.position as Map
            if (pos.lat != null) out['position.lat'] = pos.lat.toString()
            if (pos.lng != null) out['position.lng'] = pos.lng.toString()
        }

        // Online flag — Segway's REST sometimes carries it inline.
        if (status.online != null) out.online = status.online.toString()

        return out
    }

    /** Map Segway's camelCase raw state (isDocked / isRunning / …) to canonical lowercase. */
    private static String normaliseRawState(String raw) {
        if (raw == null) return null
        String trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        String mapped = RAW_STATE_TO_CANONICAL[trimmed]
        return mapped ?: trimmed.toLowerCase()
    }

    /**
     * Battery extraction mirrors mower_sdk/models.py:_extract_battery_value.
     * Returns null when no usable value found (so we don't auto-create an
     * empty 'navimow.battery' port).
     */
    private static Integer extractBattery(Map status) {
        Integer flat = toIntOrNull(status.battery)
        if (flat != null) return flat
        def capacityRemaining = status.capacityRemaining
        if (capacityRemaining instanceof List) {
            // Prefer entry whose unit == "PERCENTAGE".
            for (item in capacityRemaining) {
                if (!(item instanceof Map)) continue
                String unit = (((Map) item).unit ?: '').toString().toUpperCase()
                if (unit == 'PERCENTAGE') {
                    Integer pct = toIntOrNull(((Map) item).rawValue)
                    if (pct != null) return pct
                }
            }
            // Fallback to first entry, even if unit is missing.
            if (!capacityRemaining.isEmpty() && capacityRemaining[0] instanceof Map) {
                Integer pct = toIntOrNull(((Map) capacityRemaining[0]).rawValue)
                if (pct != null) return pct
            }
        }
        return null
    }

    private static Integer toIntOrNull(Object v) {
        if (v == null) return null
        if (v instanceof Number) return ((Number) v).intValue()
        try { return Integer.parseInt(v.toString().trim()) } catch (NumberFormatException ignored) { return null }
    }

    /** One-time-per-process raw-payload dump so we can verify the field mapping in production. */
    private static final Set<String> _DUMPED = ([] as Set).asSynchronized()
    private static void logFirstPayload(String deviceCode, Map status) {
        if (_DUMPED.add(deviceCode)) {
            log.info("NavimowInfoSyncJob first payload for ${deviceCode}: ${status}")
        }
    }

    private void publishField(Device device, String field, String value) {
        String internalRef = "${PORT_PREFIX}.${field}"
        DevicePort port = device.ports?.find { it?.internalRef == internalRef } as DevicePort
        if (port == null) {
            try {
                port = DevicePort.withNewTransaction { _ ->
                    DevicePort newPort = new DevicePort(
                            device: device,
                            type: PortType.SENSOR,
                            state: PortState.ACTIVE,
                            internalRef: internalRef,
                            name: humanize(field),
                            description: "Auto-created Navimow status field: ${field}"
                    )
                    newPort.save(flush: true, failOnError: true)
                    return newPort
                }
                device.refresh()
            } catch (Exception ex) {
                log.error("NavimowInfoSyncJob: failed to auto-create port ${internalRef}: ${ex.message}", ex)
                return
            }
        }
        try {
            // Standard cloud-device pattern (identical to MeteoStationSyncJob).
            // Publishes to myhab/<code>/mower/<port>/value; the broker echoes
            // it back via the myhab/# subscription; MQTTMessageHandler routes
            // it via NAVIMOW_READ → PortValueService.updatePort persists +
            // broadcasts to the UI WebSocket. NAVIMOW_SEGWAY must be in
            // DEVICE_TOPIC_CACHE (wired in MqttTopicService) or this silently
            // no-ops with a "topic generation failed" warning.
            mqttTopicService.publishPortValue(device, port, value)
        } catch (Exception ex) {
            log.error("NavimowInfoSyncJob: publishPortValue failed for ${internalRef}: ${ex.message}", ex)
        }
    }

    private void emitNotifications(Device device, Map<String, String> previous, Map<String, String> publishable) {
        String prevState = previous["${PORT_PREFIX}.state"]
        String prevError = previous["${PORT_PREFIX}.errorCode"]
        String prevBatteryStr = previous["${PORT_PREFIX}.battery"]
        Integer prevBattery = parseIntSafe(prevBatteryStr)

        // `publishable` already carries the normalised values produced by
        // extractPublishableFields — no need to re-parse the raw status here.
        String newState = publishable.state
        String prevStateN = normaliseState(prevState)
        String newError = publishable.errorCode
        String newErrorMessage = publishable.errorMessage
        Integer newBattery = parseIntSafe(publishable.battery)

        int threshold = readLowBatteryThreshold()
        String label = "Navimow ${device.code}"

        // 1) Errors — new errorCode that didn't exist before (or changed).
        if (newError && !newError.trim().isEmpty() && newError != prevError && newError != 'null' && newError != '0') {
            notificationService.notifyAdmins(MessageLevel.ERROR,
                    "${label}: error ${newError}",
                    "${label} reported error code ${newError}${newErrorMessage ? ' (' + newErrorMessage + ')' : ''}.",
                    'navimow')
        }

        // 2) State transitions — any change.
        if (newState && newState != prevState) {
            notificationService.notifyAdmins(MessageLevel.INFO,
                    "${label}: ${prevState ?: '?'} → ${newState}",
                    "${label} state changed from ${prevState ?: 'unknown'} to ${newState}.",
                    'navimow')
        }

        // 3) Work completed — was actively mowing, now resting, with no error.
        if (prevStateN && newState
                && ACTIVE_STATES.contains(prevStateN)
                && RESTING_STATES.contains(newState)
                && (newError == null || newError == '0' || newError.trim().isEmpty())) {
            notificationService.notifyAdmins(MessageLevel.INFO,
                    "${label}: finished mowing",
                    "${label} finished its mowing job and returned to ${newState}.",
                    'navimow')
        }

        // 4a) Low battery — crossed the threshold downward.
        if (newBattery != null && newBattery < threshold
                && (prevBattery == null || prevBattery >= threshold)) {
            notificationService.notifyAdmins(MessageLevel.WARN,
                    "${label}: low battery (${newBattery}%)",
                    "${label} battery dropped to ${newBattery}% (threshold ${threshold}%).",
                    'navimow')
        }

        // 4b) Returning to dock — caught as a state transition into 'returning'.
        if (newState && RETURNING_STATES.contains(newState) && newState != prevStateN) {
            notificationService.notifyAdmins(MessageLevel.WARN,
                    "${label}: returning to dock",
                    "${label} is returning to its dock.",
                    'navimow')
        }
    }

    /**
     * Lowercase / trim a raw state value so we can compare against the
     * canonical set without false negatives from server-side casing changes.
     */
    private static String normaliseState(Object raw) {
        if (raw == null) return null
        String s = raw.toString().trim()
        return s.isEmpty() ? null : s.toLowerCase()
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private static String readConfig(Device device, String cfgKey) {
        Configuration row = Configuration.where {
            entityId == device.id && entityType == EntityType.DEVICE && key == cfgKey
        }.find()
        String v = row?.value
        return v?.trim() ? v.trim() : null
    }

    private int readLowBatteryThreshold() {
        try {
            Integer v = configProvider?.get(Integer.class, 'navimow.battery.low.threshold')
            return v != null ? v.intValue() : DEFAULT_LOW_BATTERY_THRESHOLD
        } catch (Exception ignored) {
            return DEFAULT_LOW_BATTERY_THRESHOLD
        }
    }

    private static Integer parseIntSafe(String s) {
        if (s == null || s.trim().isEmpty()) return null
        try { return Integer.parseInt(s.trim()) } catch (NumberFormatException ignored) { return null }
    }

    private static String humanize(String camelOrSnake) {
        if (!camelOrSnake) return ''
        String spaced = camelOrSnake.replaceAll(/[_.]/, ' ').replaceAll(/([A-Z])/, ' $1').trim()
        return spaced.split(/\s+/).collect { it.capitalize() }.join(' ')
    }

    /**
     * Classify a {@link NavimowApiException} as an auth/token failure (vs a
     * transient transport error, a malformed payload, etc.). Auth failures
     * are the ones that need user action — re-running the OAuth flow — so we
     * want a dedicated notification path for them.
     *
     * <p>Triggers:</p>
     * <ul>
     *   <li>Segway envelope code {@code CODE_OAUTH_INFO_ILLEGAL} (the one we
     *       actually observed in production).</li>
     *   <li>Any {@code errorCode} containing {@code OAUTH} (defensive — Segway
     *       has historically returned variants like {@code CODE_OAUTH_*}).</li>
     *   <li>Message containing {@code HTTP 401} / {@code HTTP 403} for the
     *       transport-layer case where their gateway rejects before reaching
     *       the envelope path.</li>
     * </ul>
     */
    private static boolean isAuthFailure(NavimowApiException ex) {
        String code = (ex.errorCode ?: '').toUpperCase()
        if (code.contains('OAUTH') || code == 'TOKEN_EXPIRED') return true
        String msg = (ex.message ?: '').toUpperCase()
        return msg.contains('HTTP 401') || msg.contains('HTTP 403')
    }

    /**
     * Fire a WARN admin notification telling the user to re-authenticate.
     * Called only on the ONLINE→OFFLINE transition so we don't repeat every
     * 30s while the token stays dead.
     */
    private void notifyTokenExpired(Device device, NavimowApiException ex) {
        String label = "Navimow ${device.code}"
        String reason = ex.errorCode ?: ex.message ?: 'auth rejected'
        notificationService.notifyAdmins(MessageLevel.WARN,
                "${label}: re-authorize required",
                "${label} access token was rejected by Segway (${reason}). " +
                        "Open Devices → ${device.code} → Connect Navimow account to refresh the token.",
                'navimow')
    }

    /**
     * Flip {@code Device.status} via the standard MQTT loopback. Now that
     * NAVIMOW_SEGWAY has a STATUS_WRITE topic template registered in
     * {@link org.myhab.async.mqtt.MqttTopicService#DEVICE_TOPIC_CACHE}, the
     * publish round-trips through the broker and {@code MQTTMessageHandler}
     * routes it via the {@code NAVIMOW_STATUS} pattern into the regular
     * device-status event handler that updates the DB + broadcasts to the UI.
     */
    private void safePublishStatus(Device device, DeviceStatus status) {
        try {
            mqttTopicService.publishStatus(device, status)
        } catch (Exception ex) {
            log.error("NavimowInfoSyncJob: publishStatus(${status}) failed for ${device.code}: ${ex.message}", ex)
        }
    }
}
