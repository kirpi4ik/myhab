package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

/**
 * {@link VoiceIntentProvider} backed by the OpenAI Chat Completions API.
 *
 * <p>Uses function-calling ({@code tools} + forced {@code tool_choice}) to get
 * the same structured output as the Anthropic provider — the shared
 * {@link VoiceIntentPrompts#COMMAND_SCHEMA} is sent as the function parameters,
 * and the arguments come back as a JSON string we parse. OpenAI caches long
 * prompt prefixes automatically, so no explicit cache directive is needed.</p>
 */
@Slf4j
class OpenAiIntentProvider implements VoiceIntentProvider {

    static final String API_URL = 'https://api.openai.com/v1/chat/completions'

    @Override
    String name() { 'openai' }

    @Override
    String defaultModel() { 'gpt-4o-mini' }

    @Override
    Map resolveIntent(String transcript, List<Map> catalog, String model, String apiKey) {
        if (!apiKey?.trim()) {
            throw new IllegalStateException('OpenAI API key is not set')
        }

        String systemContent = "${VoiceIntentPrompts.SYSTEM_PROMPT}\n\n" +
                "Controllable peripherals (JSON):\n${VoiceIntentPrompts.catalogJson(catalog)}"

        Map body = [
            model      : model,
            messages   : [
                [role: 'system', content: systemContent.toString()],
                [role: 'user', content: transcript]
            ],
            tools      : [[type    : 'function',
                           function: [name       : VoiceIntentPrompts.TOOL_NAME,
                                      description : VoiceIntentPrompts.TOOL_DESCRIPTION,
                                      parameters  : VoiceIntentPrompts.COMMAND_SCHEMA]]],
            tool_choice: [type: 'function', function: [name: VoiceIntentPrompts.TOOL_NAME]]
        ]

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

        Map env = new JsonSlurper().parseText(response.body ?: '{}') as Map
        List choices = (env.choices ?: []) as List
        Map message = (choices ? choices[0].message : null) as Map
        List toolCalls = (message?.tool_calls ?: []) as List
        String args = toolCalls ? toolCalls[0].function?.arguments : null
        if (!args) {
            throw new IllegalStateException('OpenAI returned no tool call')
        }
        return new JsonSlurper().parseText(args) as Map
    }
}
