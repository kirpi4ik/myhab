package org.myhab.services.voice

/**
 * One LLM vendor (Anthropic, OpenAI, …) behind a vendor-neutral, multi-turn
 * tool-use API. {@link org.myhab.services.VoiceCommandService} owns the agentic
 * loop and the (neutral) conversation history; a provider is a pure translator:
 * neutral request in → one native round-trip → normalized {@link LlmTurn} out.
 *
 * <p>The neutral message list and the tool definitions are described in
 * {@link VoiceTools}. The system prompt and catalog JSON are passed separately
 * so a provider can place them in a cache-friendly prefix (e.g. Anthropic
 * {@code cache_control}).</p>
 */
interface VoiceIntentProvider {

    /** Lower-case provider id matching {@code feature.voice.llm.provider}. */
    String name()

    /** Model used when {@code feature.voice.llm.model} is not configured. */
    String defaultModel()

    /**
     * Run a single LLM turn.
     *
     * @param systemPrompt the shared system instructions
     * @param catalogJson  the controllable-entity catalog as JSON (cacheable)
     * @param messages     neutral conversation history (see {@link VoiceTools})
     * @param tools        neutral tool definitions ({@link VoiceTools#TOOLS})
     * @return tool calls to execute, or a final spoken text when the model is done
     */
    LlmTurn converse(String systemPrompt, String catalogJson, List<Map> messages,
                     List<Map> tools, String model, String apiKey)
}
