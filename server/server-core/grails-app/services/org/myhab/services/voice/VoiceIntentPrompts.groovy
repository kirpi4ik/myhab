package org.myhab.services.voice

import groovy.json.JsonOutput

/**
 * Vendor-neutral prompt, tool name and JSON-schema shared by every
 * {@link VoiceIntentProvider}. Keeping them here guarantees Anthropic and
 * OpenAI are asked to do exactly the same thing with the same output contract,
 * so swapping providers cannot silently change behaviour.
 */
class VoiceIntentPrompts {

    static final String TOOL_NAME = 'set_command'
    static final String TOOL_DESCRIPTION = 'Record the resolved peripheral and action for a voice command.'

    static final String SYSTEM_PROMPT = '''\
You control a home-automation system. The user speaks a single command, in any \
language (e.g. English or Romanian), to turn a peripheral on, off, or toggle it. \
You are given a JSON catalog of the peripherals that can be controlled, each with \
an "id", "name", "category" and "zones". Match the user's words to exactly one \
catalog entry and call the set_command tool with that entry's id and the requested \
action. Match by name and zone, tolerating synonyms, word order, translation and \
minor transcription errors. \
If no entry is a confident match, or the request is ambiguous between several \
entries, call set_command with a null peripheralId and an "error" explaining what \
to clarify. Never invent an id that is not in the catalog. \
spokenResponse and any error must be written in the same language the user used, \
as a short confirmation suitable to read back to the user.'''

    /** JSON Schema for the tool input — used as Anthropic input_schema and OpenAI function parameters. */
    static final Map COMMAND_SCHEMA = [
        type      : 'object',
        properties: [
            peripheralId  : [type: ['integer', 'null'],
                             description: 'The id of the matching catalog entry, or null if no confident match.'],
            action        : [type: 'string', enum: ['ON', 'OFF', 'TOGGLE'],
                             description: 'The requested action.'],
            confidence    : [type: 'number',
                             description: 'Confidence 0..1 that the match is correct.'],
            spokenResponse: [type: 'string',
                             description: 'Short confirmation to read back to the user.'],
            error         : [type: ['string', 'null'],
                             description: 'Set when nothing matched or the request is ambiguous.']
        ],
        required  : ['action']
    ]

    /** Deterministic catalog JSON (sorted by id) so it caches stably. */
    static String catalogJson(List<Map> catalog) {
        JsonOutput.toJson(catalog.sort { it.id })
    }
}
