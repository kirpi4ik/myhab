package org.myhab.services.voice

/**
 * Maps a spoken transcript to a single peripheral + action, grounded by the
 * catalog of controllable peripherals. Implementations wrap one LLM vendor
 * (Anthropic, OpenAI, …); {@link org.myhab.services.VoiceCommandService} picks
 * one by name from the {@code feature.voice.llm.provider} config and stays
 * otherwise unaware of the vendor.
 *
 * <p>All implementations must return strict structured output — never prose —
 * shaped as {@code [peripheralId, action, confidence, spokenResponse]} or
 * {@code [error: "..."]} when nothing confidently matches. The caller validates
 * the returned id against the catalog, so an implementation only has to forward
 * the model's choice, not enforce it.</p>
 */
interface VoiceIntentProvider {

    /** Lower-case provider id matching {@code feature.voice.llm.provider}. */
    String name()

    /** Model used when {@code feature.voice.llm.model} is not configured. */
    String defaultModel()

    /**
     * @param catalog list of {@code [id, name, category, zones]} maps — the only
     *                ids the model may choose from.
     * @return the structured intent map, or {@code [error: ...]}.
     */
    Map resolveIntent(String transcript, List<Map> catalog, String model, String apiKey)
}
