package org.myhab.jobs

import grails.gorm.transactions.Transactional
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.services.navimow.NavimowOAuthService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.PersistJobDataAfterExecution

/**
 * Periodic OAuth2 token refresh for every Navimow registered in the DB.
 * Mirrors {@link NibeTokenRefreshJob}'s pattern.
 *
 * <p>Segway's access tokens expire on the order of ~1h. Without this job the
 * device flips OFFLINE once the token TTL passes and the user has to re-OAuth
 * by clicking "Connect Navimow account" again. The refresh endpoint is the
 * same {@code /openapi/oauth/getAccessToken} URL we used at auth time, but
 * called with {@code grant_type=refresh_token}.</p>
 *
 * <p>OPT-IN: stays disabled unless {@code quartz.jobs.navimowTokenRefresh.enabled:
 * true} in {@code application.yml}. Default interval is 600s (10 min) — well
 * inside Segway's TTL with room for transient errors. Configurable via
 * {@code quartz.jobs.navimowTokenRefresh.interval}.</p>
 *
 * <p>Per-device failures (no refresh token stored, HTTP error, etc.) are
 * logged but never throw — one bad device shouldn't poison the whole tick.</p>
 */
@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Transactional
class NavimowTokenRefreshJob implements Job {

    NavimowOAuthService navimowOAuthService

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        def cfg = Holders.grailsApplication?.config
        Boolean enabled = cfg?.getProperty('quartz.jobs.navimowTokenRefresh.enabled', Boolean)
        if (enabled == null) enabled = false
        if (!enabled) {
            log.trace("NavimowTokenRefreshJob disabled — skipping")
            return
        }

        List<Device> mowers = Device.findAllByModel(DeviceModel.NAVIMOW_SEGWAY)
        if (mowers.isEmpty()) {
            log.trace("No NAVIMOW_SEGWAY devices registered — skipping refresh")
            return
        }

        mowers.each { Device device ->
            try {
                Map result = navimowOAuthService.refreshAccessToken(device)
                if (!result?.success) {
                    log.warn("NavimowTokenRefreshJob: refresh failed for ${device.code}: ${result?.error}")
                }
            } catch (Exception ex) {
                // Defensive — refreshAccessToken should swallow everything itself,
                // but if it throws we don't want one device to abort the whole tick.
                log.error("NavimowTokenRefreshJob: unexpected error for ${device.code}: ${ex.message}", ex)
            }
        }
    }
}
