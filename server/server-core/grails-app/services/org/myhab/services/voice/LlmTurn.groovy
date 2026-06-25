package org.myhab.services.voice

/**
 * The result of one LLM round-trip in the agentic loop: either a set of tool
 * calls to execute (then loop again with their results) or a final spoken
 * answer/question when the model is done (no tool calls).
 */
class LlmTurn {
    /** Tools the model wants executed this turn; empty when the turn is final. */
    List<ToolCall> toolCalls = []
    /** The model's natural-language text for this turn (confirmation/answer/question). */
    String finalText
}
