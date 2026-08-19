package org.myhab.domain

/**
 * Outcome of a single attempt to use a shared link.
 */
enum SharedWidgetAuditResult {
    SUCCESS,      // action was accepted and published
    DENIED_PIN,   // wrong PIN supplied
    DENIED_STATE  // link not usable (expired, disabled, archived, not yet active, limit reached)
}
