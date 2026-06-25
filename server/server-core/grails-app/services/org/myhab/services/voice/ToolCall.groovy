package org.myhab.services.voice

/**
 * One tool invocation requested by the LLM in a single turn. Vendor-neutral:
 * both Anthropic ({@code tool_use}) and OpenAI ({@code tool_calls}) shapes are
 * normalized to this by their providers.
 */
class ToolCall {
    /** Provider-assigned call id, echoed back with the tool result. */
    String id
    /** Tool name (e.g. control_entity / run_scenario / query_state). */
    String name
    /** Parsed tool arguments. */
    Map input = [:]
}
