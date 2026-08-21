package org.myhab

import groovy.util.logging.Slf4j
import org.myhab.services.DemoService

/**
 * Marks the public demo's sandbox as modified whenever a request could have changed it.
 *
 * This is what {@code DemoResetJob} reads to decide when the sandbox has gone idle,
 * which is how a shared demo gives each visitor what feels like their own copy: by the
 * time the next person arrives, the previous person's changes have been rolled back.
 *
 * Reads are ignored on purpose — someone browsing the demo should not keep an
 * abandoned set of edits alive indefinitely. GraphQL is the exception that matters:
 * every operation, query or mutation, arrives as a POST, so a visitor merely looking
 * around does count as activity there. That errs toward keeping their session intact
 * while they are using it, which is the behaviour a visitor expects.
 *
 * Outside the demo environment this is inert: DemoService.recordMutation() returns
 * immediately, and the cost is one boolean check per request.
 */
@Slf4j
class DemoActivityInterceptor {

    // After the security filters, so unauthenticated junk never marks the sandbox dirty.
    int order = LOWEST_PRECEDENCE

    DemoService demoService

    DemoActivityInterceptor() {
        matchAll()
    }

    boolean before() {
        try {
            if (request.method != 'GET' && request.method != 'HEAD' && request.method != 'OPTIONS') {
                demoService?.recordMutation()
            }
        } catch (Exception e) {
            // Never fail a request over demo bookkeeping.
            log.debug("Could not record demo activity: ${e.message}")
        }
        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
