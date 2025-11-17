package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j
import org.myhab.init.cache.CacheMap
import org.myhab.services.UserService
import org.myhab.services.SchedulerService
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
}
