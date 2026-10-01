package org.myhab.services.voice

/**
 * A fast-path stage tried before the LLM tool loop. A stage only proposes a
 * decision; VoiceCommandService applies the gate, the guards and the execution.
 */
interface IntentStage {

    String name()

    /**
     * Resolve one transcript, or return null when the stage has no answer (for
     * example a model that is not trained yet). May throw; the caller falls through.
     */
    FastDecision resolve(String transcript, String locale, Map settings)
}
