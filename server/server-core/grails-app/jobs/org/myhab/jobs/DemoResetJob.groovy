package org.myhab.jobs

import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.services.DemoService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.time.LocalTime

/**
 * Returns the public demo's shared sandbox to its seeded state.
 *
 * Two triggers, both handled here because they answer the same question — is what the
 * last visitor did still worth keeping?
 *
 * <ul>
 *   <li><b>Idle</b> — the sandbox has been modified and nobody has touched it for
 *       {@code myhab.demo.idleMinutes}. This is what makes changes feel session-scoped
 *       without per-visitor isolation: by the time the next person arrives, the
 *       previous person's edits are gone.</li>
 *   <li><b>Nightly</b> — one unconditional reset, so a sandbox kept permanently "warm"
 *       by trickling traffic still gets a clean slate once a day.</li>
 * </ul>
 *
 * Does nothing outside the demo environment: {@link DemoService#reset} refuses to run
 * without a seed schema, which production does not have.
 */
@Slf4j
@DisallowConcurrentExecution
class DemoResetJob implements Job {

    // Typed on purpose: the Quartz job factory autowires BY TYPE, so a def/Object-typed
    // property is silently skipped and stays null.
    DemoService demoService

    /** Guards against the nightly reset firing repeatedly within its hour. */
    private static volatile String lastNightlyRunDate = null

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def config = Holders.grailsApplication?.config
        if (!config?.getProperty('quartz.jobs.demoReset.enabled', Boolean, false)) {
            return
        }
        if (demoService == null) {
            log.error("DemoResetJob has no DemoService - check the bean wiring")
            return
        }
        if (!demoService.isResetAvailable()) {
            return
        }

        try {
            if (nightlyDue(config)) {
                lastNightlyRunDate = today()
                demoService.reset('nightly')
                return
            }

            int idleThreshold = config.getProperty('myhab.demo.idleMinutes', Integer, 20)
            long idleFor = demoService.idleMinutes()

            // idleFor is -1 when nothing has been modified since the last reset;
            // restoring an untouched sandbox would only churn the database.
            if (demoService.isDirty() && idleFor >= idleThreshold) {
                demoService.reset("idle for ${idleFor} minutes")
            }
        } catch (Exception e) {
            // Never let a failed reset kill the trigger - the next tick should retry.
            log.error("Demo reset job failed: ${e.message}", e)
        }
    }

    /**
     * True once per day, in the configured hour. Deliberately coarse: the exact minute
     * does not matter, and this avoids a second Quartz trigger just for the nightly run.
     */
    private boolean nightlyDue(def config) {
        int hour = config.getProperty('myhab.demo.nightlyResetHour', Integer, 4)
        return LocalTime.now().hour == hour && lastNightlyRunDate != today()
    }

    private static String today() {
        return java.time.LocalDate.now().toString()
    }
}
