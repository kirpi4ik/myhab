package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

/**
 * {@link VoiceIntentProvider} backed by the Anthropic Messages API with
 * multi-tool, multi-turn tool use. The system prompt + catalog are sent as a
 * cached block ({@code cache_control: ephemeral}); the neutral conversation is
 * translated to Anthropic content blocks per call.
 */
@Slf4j
class AnthropicIntentProvider implements VoiceIntentProvider {

    static final String API_URL = 'https://api.anthropic.com/v1/messages'
    static final String API_VERSION = '2023-06-01'

    @Override
    String name() { 'anthropic' }

    @Override
    String defaultModel() { 'claude-haiku-4-5' }

    @Override
    LlmTurn converse(String systemPrompt, String catalogJson, List<Map> messages,
                     List<Map> tools, String model, String apiKey) {
        if (!apiKey?.trim()) {
            throw new IllegalStateException('Anthropic API key is not set')
        }

        Map body = [
            model     : model,
            max_tokens: 512,
            system    : [
                [type: 'text', text: systemPrompt],
                [type: 'text', text: "Catalog (JSON):\n${catalogJson}".toString(),
                 cache_control: [type: 'ephemeral']]
            ],
            tools     : tools.collect { [name: it.name, description: it.description, input_schema: it.input_schema] },
            messages  : messages.collect { toNative(it) }
        ]

        Map env = post(body, apiKey)

        LlmTurn turn = new LlmTurn()
        List content = (env.content ?: []) as List
        StringBuilder text = new StringBuilder()
        content.each { block ->
            if (block.type == 'text') {
                text.append(block.text ?: '')
            } else if (block.type == 'tool_use') {
                turn.toolCalls << new ToolCall(id: block.id as String, name: block.name as String,
                        input: (block.input ?: [:]) as Map)
            }
        }
        turn.finalText = text.toString().trim() ?: null
        return turn
    }

    /** Translate one neutral message into an Anthropic message. */
    private static Map toNative(Map msg) {
        switch (msg.role) {
            case 'assistant':
                List blocks = []
                if (msg.text) {
                    blocks << [type: 'text', text: msg.text]
                }
                (msg.toolCalls ?: []).each { tc ->
                    blocks << [type: 'tool_use', id: tc.id, name: tc.name, input: (tc.input ?: [:])]
                }
                return [role: 'assistant', content: blocks]
            case 'tool':
                List blocks = (msg.toolResults ?: []).collect { r ->
                    [type: 'tool_result', tool_use_id: r.id, content: (r.content ?: '')]
                }
                return [role: 'user', content: blocks]
            default: // user
                return [role: 'user', content: msg.text ?: '']
        }
    }

    private static Map post(Map body, String apiKey) {
        HttpResponse<String> response
        try {
            response = VoiceHttp.INSTANCE.post(API_URL)
                    .header('x-api-key', apiKey)
                    .header('anthropic-version', API_VERSION)
                    .header('content-type', 'application/json')
                    .body(JsonOutput.toJson(body))
                    .asString()
        } catch (Exception ex) {
            throw new IllegalStateException("Anthropic transport error: ${ex.message}", ex)
        }
        if (response.status >= 400) {
            log.warn("Anthropic HTTP ${response.status}: ${response.body}")
            throw new IllegalStateException("Anthropic HTTP ${response.status}")
        }
        return new JsonSlurper().parseText(response.body ?: '{}') as Map
    }
}
