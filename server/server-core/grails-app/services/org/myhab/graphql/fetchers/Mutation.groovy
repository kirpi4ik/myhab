package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j
import org.myhab.init.cache.CacheMap
import org.myhab.domain.MessageState
import org.myhab.domain.UserMessage
import org.myhab.domain.SharedWidget
import org.myhab.domain.SharedWidgetState
import org.myhab.domain.SharedWidgetType
import org.myhab.services.UserService
import grails.plugin.springsecurity.SpringSecurityService
import org.myhab.services.SchedulerService
import org.myhab.config.ConfigProvider
import org.myhab.domain.device.Scenario
import org.myhab.domain.job.Job
import org.myhab.domain.job.EventSubscription
import org.myhab.domain.device.port.PortScenarioJoin
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@org.springframework.transaction.annotation.Transactional
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


    DataFetcher pushEvent() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def pushedEvent = environment.getArgument("input")
                publish("${pushedEvent['p0']}", pushedEvent)
                return pushedEvent
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
                    def cacheMap = hazelcastInstance.getMap(cacheName)
                    
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
                    schedulerService.triggerJob(jobId)
                    log.info("Job ${jobId} triggered successfully")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to trigger job", e)
                    return [success: false, error: e.message]
                }
            }
        }
    }

    public DataFetcher userRolesSave() {
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
     * Update an application configuration value and commit to GIT
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
     * Refresh application configuration from GIT (pull latest changes)
     */
    DataFetcher appConfigRefresh() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                try {
                    boolean success = configProvider.refresh()
                    log.info("App config refreshed from GIT")
                    return [success: true, error: null]
                } catch (Exception e) {
                    log.error("Failed to refresh app config", e)
                    return [success: false, error: e.message ?: "Failed to refresh configuration"]
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
                    String startDateStr = input.shareStartDate
                    String expireDateStr = input.shareExpireDate
                    int actionsAllowed = input.actionsAllowed as int

                    def principal = springSecurityService?.principal
                    String username = principal instanceof String ? principal : (principal?.username ?: 'unknown')

                    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    Date startDate
                    Date expireDate
                    try {
                        startDate = sdf.parse(startDateStr)
                    } catch (Exception ignored) {
                        startDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(startDateStr)
                    }
                    try {
                        expireDate = sdf.parse(expireDateStr)
                    } catch (Exception ignored) {
                        expireDate = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(expireDateStr)
                    }

                    def result = SharedWidget.withTransaction {
                        SharedWidget widget = new SharedWidget(
                            token: UUID.randomUUID().toString().replace('-', ''),
                            widgetType: SharedWidgetType.valueOf(widgetTypeStr),
                            peripheralId: peripheralId,
                            pin: (pin && !pin.trim().isEmpty()) ? pin.trim() : null,
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
