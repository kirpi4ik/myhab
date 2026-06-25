package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

/**
 * {@link VoiceIntentProvider} backed by the OpenAI Chat Completions API with
 * function-calling tool use. The neutral conversation is translated to OpenAI
 * messages per call; OpenAI caches long prompt prefixes automatically.
 */
@Slf4j
class OpenAiIntentProvider implements VoiceIntentProvider {

    static final String API_URL = 'https://api.openai.com/v1/chat/completions'

    @Override
    String name() { 'openai' }

    @Override
    String defaultModel() { 'gpt-4o-mini' }

    @Override
    LlmTurn converse(String systemPrompt, String catalogJson, List<Map> messages,
                     List<Map> tools, String model, String apiKey) {
        if (!apiKey?.trim()) {
            throw new IllegalStateException('OpenAI API key is not set')
        }

        List nativeMessages = [[role: 'system', content: "${systemPrompt}\n\nCatalog (JSON):\n${catalogJson}".toString()]]
        messages.each { nativeMessages.addAll(toNative(it)) }

        Map body = [
            model   : model,
            messages: nativeMessages,
            tools   : tools.collect {
                [type: 'function', function: [name: it.name, description: it.description, parameters: it.input_schema]]
            },
            tool_choice: 'auto'
        ]

        Map env = post(body, apiKey)

        LlmTurn turn = new LlmTurn()
        Map message = (((env.choices ?: []) as List) ? (env.choices[0].message as Map) : null)
        if (message) {
            turn.finalText = (message.content as String)?.trim() ?: null
            (message.tool_calls ?: []).each { tc ->
                Map fn = tc.function as Map
                Map input = [:]
                if (fn?.arguments) {
                    try {
                        input = new JsonSlurper().parseText(fn.arguments as String) as Map
                    } catch (Exception ignored) {
                        // Malformed tool args → leave empty; tool validation will reject it gracefully.
                        input = [:]
                    }
                }
                turn.toolCalls << new ToolCall(id: tc.id as String, name: fn?.name as String, input: input)
            }
        }
        return turn
    }

    /** Translate one neutral message into one or more OpenAI messages. */
    private static List<Map> toNative(Map msg) {
        switch (msg.role) {
            case 'assistant':
                Map m = [role: 'assistant', content: msg.text ?: null]
                if (msg.toolCalls) {
                    m.tool_calls = (msg.toolCalls).collect { tc ->
                        [id: tc.id, type: 'function',
                         function: [name: tc.name, arguments: JsonOutput.toJson(tc.input ?: [:])]]
                    }
                }
                return [m]
            case 'tool':
                return (msg.toolResults ?: []).collect { r ->
                    [role: 'tool', tool_call_id: r.id, content: (r.content ?: '')]
                }
            default: // user
                return [[role: 'user', content: msg.text ?: '']]
        }
    }

    private static Map post(Map body, String apiKey) {
        HttpResponse<String> response
        try {
            response = VoiceHttp.INSTANCE.post(API_URL)
                    .header('Authorization', "Bearer ${apiKey}")
                    .header('content-type', 'application/json')
                    .body(JsonOutput.toJson(body))
                    .asString()
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI transport error: ${ex.message}", ex)
        }
        if (response.status >= 400) {
            log.warn("OpenAI HTTP ${response.status}: ${response.body}")
            throw new IllegalStateException("OpenAI HTTP ${response.status}")
        }
        return new JsonSlurper().parseText(response.body ?: '{}') as Map
    }
}
