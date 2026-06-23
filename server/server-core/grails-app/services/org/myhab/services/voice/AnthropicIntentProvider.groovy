package org.myhab.services.voice

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import kong.unirest.HttpResponse

/**
 * {@link VoiceIntentProvider} backed by the Anthropic Messages API.
 *
 * <p>Mirrors the Unirest + JsonSlurper conventions of
 * {@link org.myhab.services.navimow.NavimowApiClient}. The model is forced to
 * emit structured output via a single tool ({@code set_command}). The system
 * prompt and the (rarely-changing) catalog are sent as a cached block
 * ({@code cache_control: ephemeral}) so repeated commands bill the static
 * context at the cache rate.</p>
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
    Map resolveIntent(String transcript, List<Map> catalog, String model, String apiKey) {
        if (!apiKey?.trim()) {
            throw new IllegalStateException('Anthropic API key is not set')
        }

        Map body = [
            model      : model,
            max_tokens : 256,
            system     : [
                [type: 'text', text: VoiceIntentPrompts.SYSTEM_PROMPT],
                [type: 'text',
                 text: "Controllable peripherals (JSON):\n${VoiceIntentPrompts.catalogJson(catalog)}".toString(),
                 cache_control: [type: 'ephemeral']]
            ],
            tools      : [[name        : VoiceIntentPrompts.TOOL_NAME,
                           description : VoiceIntentPrompts.TOOL_DESCRIPTION,
                           input_schema: VoiceIntentPrompts.COMMAND_SCHEMA]],
            tool_choice: [type: 'tool', name: VoiceIntentPrompts.TOOL_NAME],
            messages   : [[role: 'user', content: transcript]]
        ]

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

        Map env = new JsonSlurper().parseText(response.body ?: '{}') as Map
        List content = (env.content ?: []) as List
        Map toolUse = content.find { it.type == 'tool_use' } as Map
        if (!toolUse?.input) {
            throw new IllegalStateException('Anthropic returned no tool_use block')
        }
        return toolUse.input as Map
    }
}
