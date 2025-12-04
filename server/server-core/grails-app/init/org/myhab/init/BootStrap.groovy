package org.myhab.init

import groovy.util.logging.Slf4j
import kong.unirest.Unirest

import java.security.KeyStore

@Slf4j
class BootStrap {
    def telegramBotHandler
    def schedulerService
    def quartzScheduler  // Inject Quartz Scheduler
    
    def init = { servletContext ->
//        telegramBotHandler.sendMessage("INFO", "\uD83D\uDE80 Salut! sistemul myHAB tocmai a pornit")

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = "changeit";
        keystore.load(BootStrap.class.getResourceAsStream("jssecacerts"), password.toCharArray());
        Unirest.config()
                .reset()
                .socketTimeout(2000)
                .connectTimeout(2000)
                .setDefaultHeader("Accept", "application/json")
                .followRedirects(true)
                .enableCookieManagement(false)
                .verifySsl(false)
                .clientCertificateStore(keystore, password)
        
        // Clean up old Grails auto-registered jobs
        try {
            def oldGrailsJobs = quartzScheduler.getJobKeys(
                org.quartz.impl.matchers.GroupMatcher.jobGroupEquals('GRAILS_JOBS')
            )
            def oldInternalJobs = quartzScheduler.getJobKeys(
                org.quartz.impl.matchers.GroupMatcher.jobGroupEquals('Internal')
            )
            
            int deletedCount = 0
            (oldGrailsJobs + oldInternalJobs).each { jobKey ->
                if (jobKey.name.startsWith('org.myhab.jobs.')) {
                    quartzScheduler.deleteJob(jobKey)
                    deletedCount++
                }
            }
            if (deletedCount > 0) {
                log.info("Cleaned up ${deletedCount} old Grails jobs from Quartz")
            }
        } catch (Exception e) {
            log.error("Failed to clean up old Grails jobs: ${e.message}", e)
        }
        
        // Schedule all ACTIVE jobs
        try {
            schedulerService.startAll()
        } catch (Exception e) {
            log.error("Failed to schedule jobs: ${e.message}", e)
        }
        
        // Start the Quartz scheduler
        // Static jobs (NibeTokenRefreshJob, NibeInfoSyncJob) are auto-registered by Spring
        try {
            if (!quartzScheduler.isStarted()) {
                quartzScheduler.start()
                log.info("Quartz scheduler started successfully")
            }
        } catch (Exception e) {
            log.error("Failed to start Quartz scheduler: ${e.message}", e)
        }
    }
    
    def destroy = {
    }

}