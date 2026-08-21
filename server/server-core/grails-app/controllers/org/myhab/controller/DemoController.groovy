package org.myhab.controller

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import groovy.util.logging.Slf4j
import org.myhab.services.DemoService

import java.util.concurrent.atomic.AtomicLong

/**
 * The public demo's own endpoints: what the banner needs to describe itself, and the
 * "Reset demo" button behind it.
 *
 * Anonymous by necessity - the banner and the login page both render before anyone has
 * signed in. Every action first checks {@link DemoService#isDemo()}, so on a normal
 * deployment these are 404s rather than a new attack surface.
 */
@Slf4j
@Secured(['permitAll'])
class DemoController {

    static responseFormats = ['json']
    static allowedMethods = [status: 'GET', reset: 'POST']

    /** Resets are cheap but not free, and this is a public button. */
    private static final long RESET_COOLDOWN_MS = 30_000L
    private static final AtomicLong lastResetRequest = new AtomicLong(0L)

    DemoService demoService

    /**
     * GET /api/public/demo
     *
     * Drives the demo banner and the credentials shown on the login page. Returns 404
     * off the demo so the client can treat "is this the demo?" as a single question.
     */
    def status() {
        if (!demoService.isDemo()) {
            response.status = 404
            render([error: 'Not a demo deployment'] as JSON)
            return
        }

        def cfg = grailsApplication.config
        render([
                enabled     : true,
                idleMinutes : cfg.getProperty('myhab.demo.idleMinutes', Integer, 20),
                idleFor     : demoService.idleMinutes(),
                dirty       : demoService.isDirty(),
                lastReset   : demoService.lastReset(),
                resetEnabled: demoService.isResetAvailable(),
                credentials : [
                        [username: cfg.getProperty('myhab.demo.credentials.user', String, 'demo'),
                         password: cfg.getProperty('myhab.demo.credentials.userPassword', String, 'demo'),
                         role    : 'ROLE_USER'],
                        [username: cfg.getProperty('myhab.demo.credentials.admin', String, 'demo-admin'),
                         password: cfg.getProperty('myhab.demo.credentials.adminPassword', String, 'demo-admin'),
                         role    : 'ROLE_ADMIN'],
                ],
        ] as JSON)
    }

    /**
     * POST /api/public/demo/reset
     *
     * Anonymous on purpose: a visitor who has made a mess needs to be able to clear it
     * without signing in, and everything it can affect is disposable by definition.
     */
    def reset() {
        if (!demoService.isDemo()) {
            response.status = 404
            render([error: 'Not a demo deployment'] as JSON)
            return
        }

        long now = System.currentTimeMillis()
        long previous = lastResetRequest.get()
        if (now - previous < RESET_COOLDOWN_MS) {
            response.status = 429
            render([error      : 'A reset was just performed, please wait',
                    retryAfterMs: RESET_COOLDOWN_MS - (now - previous)] as JSON)
            return
        }
        // Claim the window before doing the work, so two simultaneous clicks cannot
        // both start a restore.
        if (!lastResetRequest.compareAndSet(previous, now)) {
            response.status = 429
            render([error: 'A reset is already in progress'] as JSON)
            return
        }

        try {
            boolean done = demoService.reset('requested from the demo banner')
            render([reset: done, lastReset: demoService.lastReset()] as JSON)
        } catch (Exception e) {
            log.error("Demo reset failed: ${e.message}", e)
            response.status = 500
            render([error: 'Reset failed'] as JSON)
        }
    }
}
