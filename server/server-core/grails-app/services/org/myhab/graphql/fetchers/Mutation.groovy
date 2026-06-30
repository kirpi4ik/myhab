package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat
import org.myhab.init.cache.CacheMap
import org.myhab.domain.MessageState
import org.myhab.domain.UserMessage
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.SharedWidgetType
import org.myhab.services.UserService
import grails.plugin.springsecurity.SpringSecurityService
import org.myhab.services.SchedulerService
import org.myhab.services.MegaDriverService
import org.myhab.config.ConfigProvider
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceAccount
import org.myhab.domain.device.DeviceBackup
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.NetworkAddress
import org.myhab.domain.device.Scenario
import org.myhab.domain.job.Job
import org.myhab.domain.job.EventSubscription
import org.myhab.domain.device.port.PortScenarioJoin
import org.myhab.exceptions.UnavailableDeviceException
import org.myhab.async.mqtt.MqttPublishGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Mutation implements EventPublisher {

    @Autowired
    UserService userService
    @Autowired
    HazelcastInstance hazelcastInstance
    @Autowired
    SchedulerService schedulerService
    @Autowired
    ConfigProvider configProvider
    @Autowired
    SpringSecurityService springSecurityService
    @Autowired
    MegaDriverService megaDriverService
    @Autowired
    org.myhab.services.dsl.action.NavimowCommandService navimowCommandService
    @Autowired
    org.myhab.services.navimow.NavimowOAuthService navimowOAuthService
    @Autowired
    org.myhab.services.VoiceCommandService voiceCommandService
    @Autowired
    MqttPublishGateway mqttPublishGateway


    /**
     * Resolve the authenticated username from the security context, or
     * {@code 'anonymous'} when there is no real principal. Used to attribute
     * server-side audit actions to the real user rather than trusting
     * client-supplied fields.
     */
    private String currentUsername() {
        try {
            def principal = springSecurityService?.principal
            if (principal && principal != 'anonymousUser') {
                return principal.hasProperty('username') ? principal.username : principal.toString()
            }
        } catch (Exception ignored) {
        }
        return 'anonymous'
    }

    DataFetcher pushEvent() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def pushedEvent = environment.getArgument("input")
                // Server-stamp the origin + actor: pushEvent is the web client's
                // control channel, so attribute it to WEB_UI and the authenticated
                // user rather than trusting the spoofable client-supplied p3/p6.
                pushedEvent['p3'] = 'WEB_UI'
                pushedEvent['p6'] = currentUsername()
                publish("${pushedEvent['p0']}", pushedEvent)
                return pushedEvent
            }
        }
    }

    // ==================== Device (MegaD) operations ====================

    /**
     * Read full configuration from a live MegaD controller and create a Device row.
     * On code-already-exists, surfaces existingDeviceId so the UI can offer to open
     * the existing record instead of duplicating it.
     */
    DataFetcher deviceInitFromController() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String ip = environment.getArgument("ip")
                String password = environment.getArgument("password") ?: 'sec'
                if (!ip) {
                    return [success: false, error: 'ip is required']
                }
                try {
                    Device temp = new Device(
                            code: "tmp-${ip}",
                            name: "tmp-${ip}",
                            model: DeviceModel.MEGAD_2561_RTC,
                            status: DeviceStatus.OFFLINE,
                            networkAddress: new NetworkAddress(ip: ip, port: '80')
                    )
                    temp.authAccounts = [new DeviceAccount(password: password, isDefault: true, device: temp)] as Set

                    def fullConfig = megaDriverService.readFullConfig(temp)
                    String configContent = megaDriverService.serializeFullConfig(fullConfig)
                    if (!configContent) {
                        return [success: false, error: "Could not read configuration from device at ${ip}"]
                    }

                    Device device = megaDriverService.initializeFromConfig(configContent)

                    // Code derives from controller's mdid (cf=2). Refuse on collision.
                    Device existing = Device.findByCode(device.code)
                    if (existing) {
                        return [
                                success           : false,
                                error             : "A device with code '${device.code}' already exists",
                                existingDeviceId  : existing.id,
                                existingDeviceCode: existing.code
                        ]
                    }

                    if (device.authAccounts) {
                        device.authAccounts.first().password = password
                    }
                    device.save(flush: true, failOnError: true)

                    return [
                            success   : true,
                            deviceId  : device.id,
                            deviceCode: device.code,
                            portCount : device.ports?.size() ?: 0
                    ]
                } catch (UnavailableDeviceException e) {
                    log.warn("deviceInitFromController unreachable: ip={} error={}", ip, e.message)
                    return [success: false, error: "Device at ${ip} is unreachable: ${e.message}"]
                } catch (Exception e) {
                    log.error("deviceInitFromController failed for ip={}", ip, e)
                    return [success: false, error: "Init failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Persist a temp.cf-format backup of the controller's current configuration.
     */
    DataFetcher deviceBackupConfig() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                Device device = Device.get(deviceId)
                if (!device) {
                    return [success: false, error: "Device not found: ${deviceId}"]
                }
                try {
                    DeviceBackup backup = megaDriverService.backupConfiguration(device)
                    int lines = backup.configuration ? backup.configuration.split('\n').length : 0
                    return [
                            success    : true,
                            id         : backup.id,
                            frmVersion : backup.frmVersion,
                            configLines: lines
                    ]
                } catch (UnavailableDeviceException e) {
                    return [success: false, error: "Device unreachable: ${e.message}"]
                } catch (Exception e) {
                    log.error("deviceBackupConfig failed for device id={}", deviceId, e)
                    return [success: false, error: "Backup failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Send a start / stop / pause / resume / dock command to a Segway Navimow
     * mower. Delegates to {@link org.myhab.services.dsl.action.NavimowCommandService},
     * which talks to Segway's REST API. Returns {@code success=false} with the
     * Segway error code in {@code error} when the cloud rejects the command
     * (token expired, mower already in target state, etc.).
     */
    DataFetcher mowerCommand() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                String action = environment.getArgument("action") as String
                try {
                    navimowCommandService.execute([deviceId: deviceId, action: action])
                    return [success: true, error: null]
                } catch (org.myhab.services.navimow.NavimowApiException e) {
                    log.warn("mowerCommand rejected: device=${deviceId} action=${action} code=${e.errorCode} msg=${e.message}")
                    return [success: false, error: e.message]
                } catch (Exception e) {
                    log.error("mowerCommand failed: device=${deviceId} action=${action}", e)
                    return [success: false, error: "Command failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Begin a Navimow OAuth2 flow. Returns the URL the UI should pop in a new
     * tab. The callback at /auth/external/callback finishes the exchange and
     * persists the token onto the device's Configuration sidecar — no further
     * call is needed from the client.
     */
    DataFetcher navimowOAuthStart() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                String callerOrigin = environment.getArgument("callerOrigin") as String
                try {
                    String url = navimowOAuthService.startAuthorization(deviceId, callerOrigin)
                    return [success: true, authorizeUrl: url, error: null]
                } catch (org.myhab.services.navimow.NavimowApiException e) {
                    return [success: false, authorizeUrl: null, error: e.message]
                } catch (Exception e) {
                    log.error("navimowOAuthStart failed: device=${deviceId}", e)
                    return [success: false, authorizeUrl: null, error: "OAuth start failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Finish the Navimow OAuth2 flow. Called by the SPA's
     * {@code /auth/external/callback} page after Navimow redirects the user
     * back with {@code code} and {@code state}. Wraps
     * {@link org.myhab.services.navimow.NavimowOAuthService#handleCallback}
     * so the SPA can do the exchange via GraphQL instead of via the legacy
     * controller path (which doesn't reach the backend in some reverse-proxy
     * deployments where only {@code /graphql} and SPA paths are forwarded).
     */
    DataFetcher navimowOAuthComplete() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String code = environment.getArgument("code") as String
                String state = environment.getArgument("state") as String
                try {
                    Map result = navimowOAuthService.handleCallback(code, state)
                    return [
                            success   : result.success as Boolean,
                            deviceId  : result.deviceId,
                            deviceCode: result.deviceCode,
                            error     : result.error
                    ]
                } catch (Exception e) {
                    log.error("navimowOAuthComplete failed: state=${state?.take(8)}…", e)
                    return [success: false, deviceId: null, deviceCode: null,
                            error  : "OAuth completion failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Push a stored backup down to the physical controller (with restart).
     */
    DataFetcher deviceRestoreToController() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                Long backupId = environment.getArgument("backupId") as Long
                Device device = Device.get(deviceId)
                DeviceBackup backup = backupId ? DeviceBackup.get(backupId) : null

                if (!device) {
                    return [success: false, error: "Device not found: ${deviceId}"]
                }
                if (!backup || backup.device?.id != device.id) {
                    return [success: false, error: "Backup not found for device ${deviceId}: ${backupId}"]
                }
                try {
                    megaDriverService.restoreToController(device, backup, true)
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("deviceRestoreToController failed device={} backup={}", deviceId, backupId, e)
                    return [success: false, error: "Push to controller failed: ${e.message}"]
                }
            }
        }
    }

    /**
     * Sync the database Device + DevicePort model from a stored backup.
     */
    DataFetcher deviceSyncFromBackup() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                Long deviceId = environment.getArgument("deviceId") as Long
                Long backupId = environment.getArgument("backupId") as Long
                Device device = Device.get(deviceId)
                DeviceBackup backup = backupId ? DeviceBackup.get(backupId) : null

                if (!device) {
                    return [success: false, error: "Device not found: ${deviceId}"]
                }
                if (!backup || backup.device?.id != device.id) {
                    return [success: false, error: "Backup not found for device ${deviceId}: ${backupId}"]
                }
                try {
                    megaDriverService.syncFromBackup(device, backup)
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("deviceSyncFromBackup failed device={} backup={}", deviceId, backupId, e)
                    return [success: false, error: "Sync from backup failed: ${e.message}"]
                }
            }
        }
    }

    DataFetcher cacheDelete() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def cacheName = environment.getArgument("cacheName")
                def cacheKey = environment.getArgument("cacheKey")
                def cacheKeyStr = String.valueOf(cacheKey)
                
                    // IMPORTANT: Use the cacheName argument, not hardcoded CacheMap.EXPIRE.name
                    def cacheMap = hazelcastInstance.getMap(cacheName as String)
                    
                    // Delete the entry
                    cacheMap.delete(cacheKeyStr)
                    cacheMap.flush()
                    
                    log.debug("Cache entry deleted: name=${cacheName}, key=${cacheKeyStr}")
                
                return [success: true]
            }
        }
    }

    DataFetcher jobSchedule() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long jobId = environment.getArgument("jobId") as Long
                    schedulerService.scheduleJob(jobId)
                    log.info("Job ${jobId} scheduled successfully")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to schedule job", e)
                    return [success: false, error: e.message]
                }
            }
        }
    }

    DataFetcher jobUnschedule() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long jobId = environment.getArgument("jobId") as Long
                    schedulerService.unscheduleJob(jobId)
                    log.info("Job ${jobId} unscheduled successfully")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to unschedule job", e)
                    return [success: false, error: e.message]
                }
            }
        }
    }

    DataFetcher jobTrigger() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long jobId = environment.getArgument("jobId") as Long
                    schedulerService.triggerJob(jobId, currentUsername())
                    log.info("Job ${jobId} triggered successfully")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to trigger job", e)
                    return [success: false, error: e.message]
                }
            }
        }
    }

    DataFetcher userRolesSave() {
        return userService
    }

    DataFetcher scenarioDelete() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long scenarioId = environment.getArgument("id") as Long
                    Scenario.withTransaction {
                        def scenario = Scenario.get(scenarioId)
                        if (!scenario) {
                            return [success: false, error: "Scenario with id ${scenarioId} not found"]
                        }
                        
                        // Find all jobs that reference this scenario and set scenario to null
                        def jobsUsingScenario = Job.where {
                            scenario == scenario
                        }.list()
                        
                        if (jobsUsingScenario) {
                            jobsUsingScenario.each { job ->
                                job.scenario = null
                                job.save(flush: true)
                            }
                        }
                        
                        // Find all event subscriptions that reference this scenario and delete them
                        // (scenario_id has NOT NULL constraint, so we can't set it to null)
                        def subscriptionsUsingScenario = EventSubscription.where {
                            scenario == scenario
                        }.list()
                        
                        if (subscriptionsUsingScenario) {
                            subscriptionsUsingScenario.each { subscription ->
                                subscription.delete(flush: true)
                            }
                            log.info("Deleted ${subscriptionsUsingScenario.size()} event subscriptions that referenced scenario")
                        }
                        
                        // Delete all join table records for this scenario
                        def joinRecords = PortScenarioJoin.where {
                            scenario == scenario
                        }.list()
                        
                        if (joinRecords) {
                            joinRecords.each { join ->
                                join.delete(flush: true)
                            }
                        }
                        
                        // Now delete the scenario
                        scenario.delete(flush: true)
                    }
                    log.info("Scenario ${scenarioId} deleted successfully")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to delete scenario", e)
                    return [success: false, error: e.message ?: "Failed to delete scenario"]
                }
            }
        }
    }

    /**
     * Update an application configuration value and commit to GIT. On success,
     * publish {@link org.myhab.domain.events.TopicName#EVT_APP_CFG_VALUE_CHANGED}
     * so the SPA can refresh its in-memory copy without a page reload.
     */
    DataFetcher appConfigUpdate() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    String key = environment.getArgument("key")
                    String value = environment.getArgument("value")
                    String commitMessage = environment.getArgument("commitMessage")

                    if (!key || key.trim().isEmpty()) {
                        return [success: false, error: "Configuration key is required"]
                    }

                    boolean success = configProvider.setAndCommit(key, value, commitMessage)

                    if (success) {
                        log.info("App config updated: ${key} = ${value}")
                        // Broadcast to connected SPAs so widgets reading from
                        // useAppConfigStore reflect the new value live.
                        // Payload shape mirrors EVT_CFG_VALUE_CHANGED for
                        // consistency: p4=key, p5=value.
                        publish(org.myhab.domain.events.TopicName.EVT_APP_CFG_VALUE_CHANGED.id(),
                                new org.myhab.domain.common.Event().with {
                                    p0 = org.myhab.domain.events.TopicName.EVT_APP_CFG_VALUE_CHANGED.id()
                                    p1 = 'APP_CONFIG'
                                    p4 = key
                                    p5 = value
                                    p6 = 'system'
                                    it
                                })
                        return [success: true, error: null]
                    } else {
                        return [success: false, error: "Failed to update configuration"]
                    }
                } catch (Exception e) {
                    log.error("Failed to update app config", e)
                    return [success: false, error: e.message ?: "Failed to update configuration"]
                }
            }
        }
    }

    /**
     * Publish a raw payload to an arbitrary MQTT topic. Backs the MQTT Explorer
     * "Publish" box; admin-only via the GraphQL/route security. Sends straight to
     * the broker through the existing outbound gateway — no port lookup.
     */
    DataFetcher publishMqtt() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String topic = environment.getArgument("topic")
                String payload = environment.getArgument("payload")
                if (!topic?.trim()) {
                    return [success: false, error: "topic is required"]
                }
                try {
                    mqttPublishGateway.sendToMqtt(topic, payload ?: "")
                    log.info("MQTT Explorer publish: ${topic} = ${payload}")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("publishMqtt failed for topic ${topic}", e)
                    return [success: false, error: e.message ?: "Failed to publish"]
                }
            }
        }
    }

    /**
     * Refresh application configuration from GIT (pull latest changes)
     */
    DataFetcher appConfigRefresh() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    configProvider.refresh()
                    log.info("App config refreshed from GIT")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to refresh app config", e)
                    return [success: false, error: e.message ?: "Failed to refresh configuration"]
                }
            }
        }
    }

    /**
     * Persist the global QR-code label settings to the git-backed config.
     * Writes the feature.qr.* keys via ConfigProvider.setAndCommit.
     */
    DataFetcher qrConfigUpdate() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Boolean enabled = environment.getArgument("enabled") as Boolean
                    String contentTemplate = environment.getArgument("contentTemplate") as String
                    String position = environment.getArgument("position") as String
                    Integer size = environment.getArgument("size") as Integer

                    if (!contentTemplate || contentTemplate.trim().isEmpty()) {
                        return [success: false, error: "Content template is required"]
                    }
                    String pos = position?.equalsIgnoreCase('left') ? 'left' : 'right'

                    boolean ok = configProvider.setAndCommit(org.myhab.config.CfgKey.QR.QR_ENABLED.key(), enabled.toString(), "Update QR config: enabled")
                    ok = configProvider.setAndCommit(org.myhab.config.CfgKey.QR.QR_CONTENT_TEMPLATE.key(), contentTemplate.trim(), "Update QR config: content template") && ok
                    ok = configProvider.setAndCommit(org.myhab.config.CfgKey.QR.QR_POSITION.key(), pos, "Update QR config: position") && ok
                    ok = configProvider.setAndCommit(org.myhab.config.CfgKey.QR.QR_SIZE.key(), ((size ?: 0).toString()), "Update QR config: size") && ok

                    if (ok) {
                        log.info("QR config updated: enabled=${enabled}, position=${pos}, size=${size}")
                        return [success: true, error: null]
                    }
                    return [success: false, error: "Failed to persist QR configuration"]
                } catch (Exception e) {
                    log.error("Failed to update QR config", e)
                    return [success: false, error: e.message ?: "Failed to update QR configuration"]
                }
            }
        }
    }

    /**
     * Resolve a transcribed voice phrase to a peripheral + action and execute it.
     * Delegates to {@link org.myhab.services.VoiceCommandService}, which grounds
     * the LLM with the live peripheral catalog, validates the resolved id, and
     * publishes an {@code evt_switch} event (handled by UIMessageService).
     */
    DataFetcher voiceCommand() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                String transcript = environment.getArgument("transcript") as String
                String locale = environment.getArgument("locale") as String
                String sessionId = environment.getArgument("sessionId") as String
                String username = 'voice'
                try {
                    def principal = springSecurityService?.principal
                    username = principal instanceof String ? principal : (principal?.username ?: 'voice')
                } catch (Exception ignored) { /* unauthenticated fallback */ }
                try {
                    return voiceCommandService.handleTranscript(transcript, locale, sessionId, username)
                } catch (Exception e) {
                    log.error("voiceCommand failed for transcript='${transcript}'", e)
                    return [success: false, error: "Voice command failed: ${e.message}",
                            transcript: transcript, spokenResponse: null, sessionId: sessionId,
                            awaitingReply: false, actions: []]
                }
            }
        }
    }

    /**
     * Set the authenticated user's preferred UI language. Self-scoped — always
     * updates the principal's own User row. A null/blank value clears the
     * preference so the client falls back to the browser language.
     */
    DataFetcher meUpdateLanguage() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    String language = environment.getArgument("language") as String
                    def principal = springSecurityService?.principal
                    String username = principal instanceof String ? principal : (principal?.username ?: principal?.toString())
                    if (!username) {
                        return [success: false, error: "Not authenticated"]
                    }
                    org.myhab.domain.User.withTransaction {
                        org.myhab.domain.User user = org.myhab.domain.User.findByUsername(username)
                        if (!user) {
                            return [success: false, error: "User not found"]
                        }
                        user.language = language?.trim() ? language.trim() : null
                        user.save(flush: true, failOnError: true)
                    }
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to update user language", e)
                    return [success: false, error: e.message ?: "Failed to update language"]
                }
            }
        }
    }

    DataFetcher messageUpdateState() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long msgId = environment.getArgument("id") as Long
                    String stateStr = environment.getArgument("state")
                    MessageState newState = MessageState.valueOf(stateStr)

                    UserMessage.withTransaction {
                        def msg = UserMessage.get(msgId)
                        if (!msg) {
                            return [success: false, error: "Message not found"]
                        }
                        msg.state = newState
                        msg.save(flush: true, failOnError: true)
                    }
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to update message state", e)
                    return [success: false, error: e.message ?: "Failed to update message state"]
                }
            }
        }
    }

    DataFetcher sharedWidgetCreate() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    def input = environment.getArgument("input")
                    String widgetTypeStr = input.widgetType
                    String peripheralId = input.peripheralId
                    String pin = input.pin
                    String description = input.description
                    String startDateStr = input.shareStartDate
                    String expireDateStr = input.shareExpireDate
                    int actionsAllowed = input.actionsAllowed as int

                    def principal = springSecurityService?.principal
                    String username = principal instanceof String ? principal : (principal?.username ?: 'unknown')

                    def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    Date startDate
                    Date expireDate
                    try {
                        startDate = sdf.parse(startDateStr)
                    } catch (Exception ignored) {
                        startDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDateStr)
                    }
                    try {
                        expireDate = sdf.parse(expireDateStr)
                    } catch (Exception ignored) {
                        expireDate = new SimpleDateFormat("yyyy-MM-dd").parse(expireDateStr)
                    }

                    def result = SharedWidget.withTransaction {
                        SharedWidget widget = new SharedWidget(
                            token: UUID.randomUUID().toString().replace('-', ''),
                            widgetType: SharedWidgetType.valueOf(widgetTypeStr),
                            peripheralId: peripheralId,
                            pin: (pin && !pin.trim().isEmpty()) ? pin.trim() : null,
                            description: (description && !description.trim().isEmpty()) ? description.trim() : null,
                            shareStartDate: startDate,
                            shareExpireDate: expireDate,
                            actionsAllowed: actionsAllowed,
                            actionsUsed: 0,
                            state: SharedWidgetState.VALID,
                            createdByUsername: username
                        )
                        widget.save(flush: true, failOnError: true)
                        return [token: widget.token]
                    }
                    return [
                        success : true,
                        error   : null,
                        token   : result.token,
                        shareUrl: "/shared/${result.token}"
                    ]
                } catch (Exception e) {
                    log.error("Failed to create shared widget", e)
                    return [success: false, error: e.message ?: "Failed to create shared widget", token: null, shareUrl: null]
                }
            }
        }
    }

    DataFetcher sharedWidgetUpdateState() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    Long widgetId = environment.getArgument("id") as Long
                    String stateStr = environment.getArgument("state")
                    String stateDescription = environment.getArgument("stateDescription")
                    SharedWidgetState newState = SharedWidgetState.valueOf(stateStr)

                    SharedWidget.withTransaction {
                        def widget = SharedWidget.get(widgetId)
                        if (!widget) {
                            return [success: false, error: "Shared widget not found"]
                        }
                        widget.state = newState
                        if (stateDescription != null) {
                            widget.stateDescription = stateDescription
                        }
                        widget.save(flush: true, failOnError: true)
                    }
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to update shared widget state", e)
                    return [success: false, error: e.message ?: "Failed to update shared widget state"]
                }
            }
        }
    }

    DataFetcher messageBatchUpdateState() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    List<String> ids = environment.getArgument("ids")
                    String stateStr = environment.getArgument("state")
                    MessageState newState = MessageState.valueOf(stateStr)

                    UserMessage.withTransaction {
                        ids.each { idStr ->
                            Long msgId = idStr as Long
                            def msg = UserMessage.get(msgId)
                            if (msg) {
                                msg.state = newState
                                msg.save(flush: true, failOnError: true)
                            }
                        }
                    }
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to batch update message states", e)
                    return [success: false, error: e.message ?: "Failed to batch update message states"]
                }
            }
        }
    }
}
