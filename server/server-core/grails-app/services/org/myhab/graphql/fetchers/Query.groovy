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
import org.myhab.domain.device.DeviceModel
import org.myhab.init.cache.CacheMap
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
    HazelcastInstance hazelcastInstance;
    @Autowired
    ConfigProvider configProvider
    
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    def userRolesForUser() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def userId = environment.getArgument("userId")
                def user = User.findById(userId)
                def userRoles = user.authorities
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
                def cacheName = environment.getArgument("cacheName")
                def cacheKey = environment.getArgument("cacheKey")
                // Convert cacheKey to String to match mutation's String.valueOf()
                def cachedValue = hazelcastInstance.getMap(cacheName).get(String.valueOf(cacheKey))
                // Return actual null instead of string "null" when cache is empty
                def expireOn = cachedValue ? cachedValue["expireOn"] : null
                return [cacheName: cacheName, cacheKey: cacheKey, cachedValue: expireOn]

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
                        // Return actual null instead of string "null" when cache is empty
                        def expireOn = entry.value ? entry.value["expireOn"] : null
                        result << [cacheName: cacheName, cacheKey: entry.key, cachedValue: expireOn]
                    }
                } else {
                    // If no cache name provided, query all caches
                    CacheMap.values().each { cMap ->
                        hazelcastInstance.getMap(cMap.name).entrySet().each { entry ->
                            // Return actual null instead of string "null" when cache is empty
                            def expireOn = entry.value ? entry.value["expireOn"] : null
                            result << [cacheName: cMap.name, cacheKey: entry.key, cachedValue: expireOn]
                        }
                    }
                }
                return result
            }
        }
    }

    def config() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def key = environment.getArgument("key")
                def config = configProvider.get(String, key)
                return config
            }
        }
    }

    /**
     * Get all application configuration entries from GIT
     */
    def appConfigList() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                return configProvider.getAll()
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
                    state == JobState.ACTIVE && peripheral != null
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

}
