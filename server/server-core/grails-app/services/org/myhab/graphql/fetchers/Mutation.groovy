package org.myhab.graphql.fetchers

import com.hazelcast.core.HazelcastInstance
import grails.events.EventPublisher
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.util.logging.Slf4j
import org.myhab.init.cache.CacheMap
import org.myhab.services.UserService
import org.myhab.services.SchedulerService
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
                hazelcastInstance.getMap(CacheMap.EXPIRE.name).delete(String.valueOf(cacheKey))
                hazelcastInstance.getMap(CacheMap.EXPIRE.name).flush()
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

    public DataFetcher userRolesSave() {
        return userService
    }
}
