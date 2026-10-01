package org.myhab.services.voice

/**
 * One fast-path stage's reading of a transcript. Targets are catalog keys:
 * {@code P<peripheralId>} or {@code Z<zoneId>}; scenarios are {@code S<jobId>}.
 */
class FastDecision {
    String stage
    /** control | scenario | query | mower | clarify | other */
    String intent
    String target
    /** ON | OFF | TOGGLE */
    String action
    String scenario
    /** START | STOP | PAUSE | RESUME | DOCK */
    String mowerAction
    /** Lowest confidence among the answers the intent needs. */
    double confidence
    /** Catalog hash the stage's model was built from, when it reports one. */
    String catalogHash

    /** Compact form for logs and shadow comparison, e.g. {@code control|P12|ON}. */
    String key() {
        [intent, target ?: scenario, action ?: mowerAction].findAll { it }.join('|')
    }
}
