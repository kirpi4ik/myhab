package org.myhab.init

import groovy.util.logging.Slf4j
import kong.unirest.Unirest
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType

import java.security.KeyStore

@Slf4j
class BootStrap {
    def telegramBotHandler
    def schedulerService
    def quartzScheduler  // Inject Quartz Scheduler

    /**
     * UI configuration defaults. Seeded once on first startup so existing
     * deployments keep working after `process.env.*` is removed from the
     * frontend. Each row is created with {@code findOrCreate} semantics —
     * if a row already exists for the key it's left untouched, so operators
     * can override values via the ConfigurationView UI without us clobbering
     * them on every restart.
     *
     * The values mirror what used to live in {@code client/web-vue3/.env}
     * before the migration. {@code null} means "no sensible default — leave
     * the row absent so the UI gets back null and components can render an
     * empty state instead of pointing at a bogus id".
     */
    private static final Map<String, String> UI_CONFIG_DEFAULTS = [
            'ui.device.door_lock.id'           : '3599655',
            'ui.device.water_pump.id'          : '4131752',
            'ui.device.heat_pump.id'           : '1002',
            'ui.device.electric_meter_01.id'   : '3805577',
            'ui.device.navimow.id'             : '18986468',
            'ui.device.meteo_station.id'       : '2000',
            'ui.device.solar_plant.id'         : '1000',
            'ui.device.solar_meter.id'         : '1001',
            'ui.zone.int.id'                   : '1903',
            'ui.zone.ext.id'                   : '1924',
            'ui.zone.etaj.id'                  : '1914',
            'ui.zone.parter.id'                : '1904',
            'ui.zone.lan.id'                   : '1940',
            'ui.zone.garden.id'                : '1941',
            'ui.grafana.url'                   : 'https://grafana.madhouse.app',
            'ui.grafana.dashboard.solar.id'    : 'adsv8rl',
            'ui.date.format.long'              : 'yyyy/MM/dd HH:mm',
    ]

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

        seedUiConfigDefaults()
    }

    /**
     * Upsert UI config rows from {@link #UI_CONFIG_DEFAULTS}. Insert any
     * missing keys; update existing keys whose stored value differs from the
     * default. This makes BootStrap.groovy the source of truth for ui.*
     * defaults — editing a value here and restarting the backend propagates
     * to the database. Operators can still edit values at runtime via the
     * Configuration UI, but those edits are reverted on the next restart
     * unless the BootStrap default is updated to match.
     */
    private void seedUiConfigDefaults() {
        try {
            Configuration.withNewTransaction {
                int inserted = 0
                int updated = 0
                UI_CONFIG_DEFAULTS.each { String key, String value ->
                    Configuration existing = Configuration.findByEntityTypeAndEntityIdAndKey(
                            EntityType.CONFIG, 0L, key)
                    if (existing == null) {
                        new Configuration(
                                entityType: EntityType.CONFIG,
                                entityId: 0L,
                                key: key,
                                value: value
                        ).save(flush: true, failOnError: true)
                        inserted++
                    } else if (existing.value != value) {
                        existing.value = value
                        existing.save(flush: true, failOnError: true)
                        // Configuration.beforeUpdate publishes EVT_CFG_VALUE_CHANGED,
                        // which the SPA picks up via its WebSocket listener — so
                        // already-connected dashboards reflect the new value live.
                        updated++
                    }
                }
                if (inserted > 0 || updated > 0) {
                    log.info("UI config sync: ${inserted} inserted, ${updated} updated")
                }
            }
        } catch (Exception e) {
            log.error("Failed to seed UI config defaults: ${e.message}", e)
        }
    }
    
    def destroy = {
    }

}