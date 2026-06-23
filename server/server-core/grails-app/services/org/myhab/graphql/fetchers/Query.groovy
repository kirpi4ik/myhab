package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import org.myhab.ConfigKey
import org.myhab.config.ConfigProvider
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.User
import org.myhab.domain.job.Job
import org.myhab.domain.job.JobExecutionHistory
import org.myhab.domain.job.JobState
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceBackup
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.MessageLevel
import org.myhab.domain.MessageState
import org.myhab.domain.UserMessage
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.device.DevicePeripheral
import org.myhab.init.cache.CacheMap
import org.myhab.services.MegaDriverService
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Query {
    @Autowired
    HazelcastInstance hazelcastInstance
    @Autowired
    ConfigProvider configProvider
    @Autowired
    SpringSecurityService springSecurityService
    @Autowired
    MegaDriverService megaDriverService
    @Autowired
    org.myhab.async.mqtt.MqttRetainedFetchService mqttRetainedFetchService
    @Autowired
    org.myhab.services.LabelService labelService

    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    def userRolesForUser() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def userId = environment.getArgument("userId")
                def user = User.findById(userId)
                def response = []
                user.authorities.each {
                    response << [userId: user.id, roleId: it.id]
                }
                return response
            }


        }
    }

    def cache() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def cacheName = environment.getArgument("cacheName") as String
                def cacheKey = environment.getArgument("cacheKey")
                // Convert cacheKey to String to match mutation's String.valueOf()
                def cachedValue = hazelcastInstance.getMap(cacheName).get(String.valueOf(cacheKey))
                return [cacheName: cacheName, cacheKey: cacheKey, cachedValue: extractExpireOn(cachedValue)]

            }
        }
    }

    def cacheAll() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String cacheName = environment.getArgument("cacheName")
                def result = []

                if (cacheName) {
                    // If specific cache name is provided, only query that cache
                    hazelcastInstance.getMap(cacheName).entrySet().each { entry ->
                        result << [cacheName: cacheName, cacheKey: entry.key, cachedValue: extractExpireOn(entry.value)]
                    }
                } else {
                    // If no cache name provided, query all caches
                    CacheMap.values().each { cMap ->
                        hazelcastInstance.getMap(cMap.name).entrySet().each { entry ->
                            result << [cacheName: cMap.name, cacheKey: entry.key, cachedValue: extractExpireOn(entry.value)]
                        }
                    }
                }
                return result
            }
        }
    }

    /**
     * Pull the {@code expireOn} timestamp out of a Hazelcast cache entry value.
     * The EXPIRE / OAUTH_STATE / TOKENS maps store {@code [expireOn: ..., ...]}
     * Map values, but the NOTIFICATION_DEDUP map (added with the per-key
     * cooldown work) stores a bare {@code Long} — relying on
     * {@code value['expireOn']} via Groovy property access throws
     * {@code No such property: expireOn for class: java.lang.Long}, which
     * surfaced as an intermittent GraphQL error on ZoneCombinedView / dashboard
     * cards every time {@code cacheAll} iterated all maps. Treat non-Map values
     * as "no expireOn" instead of failing the whole query.
     */
    private static Object extractExpireOn(Object cachedValue) {
        if (cachedValue == null) return null
        if (cachedValue instanceof Map) return cachedValue.get('expireOn')
        // Bare scalars (e.g. NOTIFICATION_DEDUP's timestamp Long) have no
        // expireOn property. Hazelcast's per-entry TTL handles their lifetime,
        // and they're not user-facing — return null cleanly.
        return null
    }

    def config() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String key = environment.getArgument("key") as String
                def config = configProvider.get(String, key)
                return config
            }
        }
    }

    /** Global QR-code label settings for the QR settings page. */
    def qrConfig() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return labelService.getQrConfig()
            }
        }
    }

    /**
     * Get all application configuration entries from GIT. Includes secrets
     * (mqtt.password, telegram.token, opsgenie.apiKey, …) so use the
     * narrower `uiConfigList` for non-admin SPA bootstrapping.
     */
    def appConfigList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return configProvider.getAll()
            }
        }
    }

    /**
     * UI-safe subset of {@link #appConfigList}. The SPA calls this once at
     * boot and hydrates the `useAppConfigStore` Pinia store, so widgets read
     * device/zone IDs (and other UI defaults) synchronously after boot.
     * Edits flow through the standard `appConfigUpdate` mutation; the git
     * repo is the long-term source of truth.
     *
     * We expose this separately from `appConfigList` because the broader
     * config tree contains secrets (mqtt.password, telegram.token,
     * opsgenie.apiKey, …). The allowlist below names only the top-level
     * branches whose contents are safe to ship to every authenticated user:
     *   - {@code specialDevices.*} — well-known device/peripheral IDs
     *     (door lock, water pump, heat pump, navimow, etc.)
     *   - {@code specialZones.*}   — well-known zone IDs (int/ext/etaj/
     *     parter/lan/garden) referenced by the dashboard quick-access cards
     *   - {@code grafana.*}        — public Grafana embed URL + dashboard
     *     IDs (no API keys)
     *   - {@code surveillance.*}   — public video stream URL
     *   - {@code ui.*}             — pure-UI formatting (date format, etc.)
     *
     * Add a new top-level branch only if every key under it is safe for
     * non-admin users to read.
     */
    private static final List<String> UI_SAFE_PREFIXES = [
            'specialDevices.',
            'specialZones.',
            'grafana.',
            'surveillance.',
            'ui.',
    ].asImmutable()

    def uiConfigList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return configProvider.getAll()
                        .findAll { Map row ->
                            String key = row?.key
                            key != null && UI_SAFE_PREFIXES.any { String p -> key.startsWith(p) }
                        }
                        .collect { Map row -> [key: row.key, value: row.value] }
            }
        }
    }

    def deviceModelList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return DeviceModel.values()
            }
        }
    }

    /**
     * Fetch job execution history for a specific job ID.
     * Returns execution history ordered by start time descending (most recent first).
     */
    def jobExecutionHistoryByJobId() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def jobId = environment.getArgument("jobId")
                def limit = environment.getArgument("limit") ?: 50
                
                if (!jobId) {
                    return []
                }
                
                // Query by job_id (for dynamic jobs) or by jobName pattern (for dynamic jobs without job reference)
                def historyList = JobExecutionHistory.createCriteria().list(max: limit) {
                    or {
                        eq('job.id', Long.parseLong(jobId.toString()))
                        like('jobName', "job_${jobId}")
                    }
                    order('startTime', 'desc')
                }
                
                return historyList.collect { history ->
                    [
                        id: history.id,
                        jobId: history.job?.id,
                        jobName: history.jobName,
                        jobGroup: history.jobGroup,
                        triggerName: history.triggerName,
                        triggerGroup: history.triggerGroup,
                        fireInstanceId: history.fireInstanceId,
                        startTime: history.startTime ? ISO_DATE_FORMAT.format(history.startTime) : null,
                        endTime: history.endTime ? ISO_DATE_FORMAT.format(history.endTime) : null,
                        durationMs: history.durationMs,
                        status: history.status?.name(),
                        errorMessage: history.errorMessage,
                        exceptionClass: history.exceptionClass,
                        recovering: history.recovering,
                        refireCount: history.refireCount,
                        scheduledFireTime: history.scheduledFireTime ? ISO_DATE_FORMAT.format(history.scheduledFireTime) : null,
                        actualFireTime: history.actualFireTime ? ISO_DATE_FORMAT.format(history.actualFireTime) : null,
                        tsCreated: history.tsCreated ? ISO_DATE_FORMAT.format(history.tsCreated) : null,
                        tsUpdated: history.tsUpdated ? ISO_DATE_FORMAT.format(history.tsUpdated) : null
                    ]
                }
            }
        }
    }

    /**
     * Active jobs that have a linked sprinkler peripheral (for scheduler UI).
     * Ordered by peripheral name. Timeout from key.on.timeout in seconds, default 60 minutes.
     */
    def sprinklerScheduleJobs() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def jobs = Job.where {
                    peripheral != null
                }.list(sort: 'peripheral.name', order: 'asc')
                return jobs.collect { job ->
                    def peripheral = job.peripheral
                    def timeoutConfig = Configuration.findByEntityIdAndEntityTypeAndKey(
                        peripheral.id, EntityType.PERIPHERAL, ConfigKey.STATE_ON_TIMEOUT
                    )
                    int timeoutSeconds = timeoutConfig?.value ? Integer.parseInt(timeoutConfig.value.trim()) : 3600
                    int timeoutMinutes = Math.max(1, (int) (timeoutSeconds / 60))
                    [
                        jobId: job.id,
                        jobName: job.name,
                        jobState: job.state?.name(),
                        peripheralId: peripheral.id,
                        peripheralName: peripheral.name ?: '',
                        peripheralDescription: peripheral.description,
                        timeoutMinutes: timeoutMinutes,
                        cronTriggers: (job.cronTriggers ?: []).collect { t ->
                            [id: t.id, expression: t.expression]
                        }
                    ]
                }
            }
        }
    }

    private User resolveCurrentUser() {
        def principal = springSecurityService?.principal
        if (!principal) return null
        String username = principal instanceof String ? principal : (principal?.username ?: principal?.toString())
        return User.findByUsername(username)
    }

    def myMessages() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def currentUser = resolveCurrentUser()
                if (!currentUser) return []

                String levelArg = environment.getArgument("level")
                String stateArg = environment.getArgument("state")

                def criteria = UserMessage.createCriteria()
                def results = criteria.list {
                    eq('user', currentUser)
                    if (levelArg) {
                        eq('level', MessageLevel.valueOf(levelArg))
                    }
                    if (stateArg) {
                        eq('state', MessageState.valueOf(stateArg))
                    }
                    order('tsCreated', 'desc')
                }

                return results.collect { msg ->
                    [
                        id        : msg.id,
                        subject   : msg.subject,
                        fromSender: msg.fromSender,
                        message   : msg.message,
                        level     : msg.level?.name(),
                        state     : msg.state?.name(),
                        tsCreated : msg.tsCreated ? ISO_DATE_FORMAT.format(msg.tsCreated) : null,
                        tsUpdated : msg.tsUpdated ? ISO_DATE_FORMAT.format(msg.tsUpdated) : null
                    ]
                }
            }
        }
    }

    def myUnreadCount() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def currentUser = resolveCurrentUser()
                if (!currentUser) return 0
                return UserMessage.countByUserAndState(currentUser, MessageState.NEW)
            }
        }
    }

    def sharedWidgets() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String stateArg = environment.getArgument("state")

                def criteria = SharedWidget.createCriteria()
                def results = criteria.list {
                    if (stateArg) {
                        eq('state', SharedWidgetState.valueOf(stateArg))
                    }
                    order('tsCreated', 'desc')
                }

                return results.collect { sw ->
                    String peripheralName = ''
                    try {
                        def peripheral = DevicePeripheral.get(sw.peripheralId)
                        peripheralName = peripheral?.name ?: ''
                    } catch (ignored) {}

                    [
                        id               : sw.id,
                        token            : sw.token,
                        widgetType       : sw.widgetType?.name(),
                        peripheralId     : sw.peripheralId,
                        peripheralName   : peripheralName,
                        shareStartDate   : sw.shareStartDate ? ISO_DATE_FORMAT.format(sw.shareStartDate) : null,
                        shareExpireDate  : sw.shareExpireDate ? ISO_DATE_FORMAT.format(sw.shareExpireDate) : null,
                        actionsAllowed   : sw.actionsAllowed,
                        actionsUsed      : sw.actionsUsed,
                        state            : sw.state?.name(),
                        description      : sw.description,
                        stateDescription : sw.stateDescription,
                        createdByUsername: sw.createdByUsername,
                        hasPin           : sw.pin != null && !sw.pin.isEmpty(),
                        tsCreated        : sw.tsCreated ? ISO_DATE_FORMAT.format(sw.tsCreated) : null,
                        tsUpdated        : sw.tsUpdated ? ISO_DATE_FORMAT.format(sw.tsUpdated) : null
                    ]
                }
            }
        }
    }

    // ==================== Current user ====================

    /**
     * Returns id + username of the authenticated user (via Spring Security principal).
     * Used by the SPA on app load to populate `currentUser` for avatar / profile rendering.
     */
    DataFetcher me() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def principal = springSecurityService?.principal
                if (!principal) {
                    return null
                }
                String username = principal instanceof String ? principal : (principal?.username ?: principal?.toString())
                User user = User.findByUsername(username)
                if (!user) {
                    return null
                }
                return [id: user.id, username: user.username]
            }
        }
    }

    // ==================== Device (MegaD) operations ====================

    /**
     * UDP-broadcast scan for MegaD controllers on the local network.
     */
    DataFetcher discoveredDevices() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return megaDriverService.discoverDevices(true)
            }
        }
    }

    /**
     * Backups stored in the DB for a given device, newest first.
     */
    DataFetcher deviceBackupList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                Device device = Device.get(deviceId)
                if (!device) {
                    return []
                }
                Date epoch = new Date(0)
                return (device.backups ?: [])
                        .sort { a, b -> (b?.tsCreated ?: epoch) <=> (a?.tsCreated ?: epoch) }
                        .collect { DeviceBackup b ->
                            [
                                    id         : b.id,
                                    frmVersion : b.frmVersion,
                                    configLines: b.configuration ? b.configuration.split('\n').length : 0,
                                    tsCreated  : b.tsCreated ? ISO_DATE_FORMAT.format(b.tsCreated) : null
                            ]
                        }
            }
        }
    }

    /**
     * On-demand fetch of a device's current IP from the MQTT broker, without
     * requiring the corresponding DevicePort to exist in the DB yet. Maps the
     * device's model to the appropriate IP-broadcast topic and subscribes
     * briefly via {@link org.myhab.async.mqtt.MqttRetainedFetchService}.
     *
     * <p>Returns a {@code DeviceIpFromMqttResult} with:</p>
     * <ul>
     *   <li>{@code topic} — the topic that was subscribed to (always populated for traceability)</li>
     *   <li>{@code ip} — the trimmed payload value, or {@code null} on timeout / device not found</li>
     *   <li>{@code error} — non-null message when the device or model is unsupported, or broker access fails</li>
     * </ul>
     */
    DataFetcher deviceFetchIpFromMqtt() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String deviceCode = environment.getArgument("deviceCode") as String
                if (!deviceCode?.trim()) {
                    return [topic: '', ip: null, error: 'deviceCode is required']
                }
                Device device = Device.findByCode(deviceCode.trim())
                if (!device) {
                    return [topic: '', ip: null, error: "Device '${deviceCode}' not found"]
                }
                String portCode = portCodeForIpBroadcast(device.model)
                String topic = topicForIpBroadcast(device.model, device.code)
                if (!topic || !portCode) {
                    return [topic: '', ip: null,
                            error: "Model ${device.model} has no MQTT IP-broadcast topic configured on the server"]
                }

                // 1. In-memory cache populated by the existing inbound MQTT channel —
                //    works for non-retained messages too, as long as the backend has
                //    observed at least one publish since startup.
                String cached = mqttRetainedFetchService.getCachedValue(device.code, portCode)
                if (cached?.trim()) {
                    return [topic: topic, ip: cached.trim(), error: null]
                }

                // 2. Cache miss → transient broker subscription. Picks up retained
                //    messages immediately; non-retained ones if the device republishes
                //    within the window.
                String raw = mqttRetainedFetchService.fetchLatestValue(topic, 5000L)
                return [topic: topic, ip: raw?.trim() ?: null,
                        error: raw == null ? "No MQTT payload received on ${topic} within timeout (broker has no retained value and the device didn't publish in the last 5s)" : null]
            }
        }
    }

    /**
     * Map a device model to the topic the device publishes its current IP on.
     * Returns {@code null} for models with no known IP-broadcast convention.
     */
    private String topicForIpBroadcast(DeviceModel model, String deviceCode) {
        if (!model || !deviceCode) return null
        String portCode = portCodeForIpBroadcast(model)
        if (!portCode) return null
        switch (model) {
            case DeviceModel.ESP32:
                return "myhab/${deviceCode}/sensor/${portCode}/state".toString()
            default:
                return null
        }
    }

    /** The {@code portCode} that the IP-broadcast topic exposes for a given model. */
    private String portCodeForIpBroadcast(DeviceModel model) {
        if (!model) return null
        switch (model) {
            case DeviceModel.ESP32:
                return 'esp_ip_address'
            default:
                return null
        }
    }

}
