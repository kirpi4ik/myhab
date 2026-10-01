package org.myhab.jobs

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.myhab.services.VoiceCommandService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.beans.factory.annotation.Autowired

/**
 * Keeps the voice NLU sidecar trained on the current catalog: sends it the catalog
 * whenever the catalog's hash differs from the one its heads were trained on. A hash
 * check covers every way the catalog changes (peripherals, zones, aliases, scenarios).
 *
 * Configuration:
 * - quartz.jobs.voiceNluSync.enabled: true/false (default: false)
 * - quartz.jobs.voiceNluSync.interval: seconds (default: 300)
 * Does nothing unless feature.voice.nlu.enabled is set.
 */
@Slf4j
@DisallowConcurrentExecution
@Transactional(readOnly = true)
class NluCatalogSyncJob implements Job {

    @Autowired
    VoiceCommandService voiceCommandService

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            voiceCommandService.syncNluCatalog()
        } catch (Exception e) {
            log.warn("Voice NLU catalog sync failed: ${e.message}")
        }
    }
}
