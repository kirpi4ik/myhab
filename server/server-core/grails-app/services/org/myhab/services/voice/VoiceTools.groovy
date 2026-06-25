package org.myhab.services.voice

import groovy.json.JsonOutput

/**
 * Vendor-neutral tool definitions, system prompt and catalog serialization for
 * the agentic voice loop. Both providers translate {@link #TOOLS} into their own
 * tool/function format, so Anthropic and OpenAI expose exactly the same tools.
 *
 * <p>Neutral message shape used across the loop (see VoiceCommandService):</p>
 * <ul>
 *   <li>{@code [role:'user', text:String]}</li>
 *   <li>{@code [role:'assistant', text:String|null, toolCalls:[ToolCall-like maps]]}</li>
 *   <li>{@code [role:'tool', toolResults:[[id, name, content:String]]]}</li>
 * </ul>
 */
class VoiceTools {

    static final String CONTROL_ENTITY = 'control_entity'
    static final String RUN_SCENARIO = 'run_scenario'
    static final String QUERY_STATE = 'query_state'
    static final String MOWER_COMMAND = 'mower_command'

    static final String SYSTEM_PROMPT = '''\
You are the voice assistant of a home-automation system. The user speaks short \
commands or questions in any language (often English or Romanian). You are given \
a JSON catalog of controllable peripherals, zones (areas/rooms, each listing the \
peripherals inside it), runnable scenarios and robotic lawn mowers. Use the tools \
to act, then give a brief spoken reply.

Rules:
- Match the user's words (names, zones, aliases) to catalog entries, tolerating \
synonyms, translation, word order and transcription errors. Never use an id that \
is not in the catalog.
- When the user refers to a place/area (e.g. "the terrace", "downstairs") and it \
maps to a zone, control the whole ZONE with control_entity(entityType:"ZONE"), \
not a single peripheral — unless they clearly name one specific device.
- If the request is genuinely ambiguous (several distinct matches and you cannot \
tell which), do NOT guess: reply with a short clarifying question and call no \
tool. The user will answer.
- For "run/start <name>" of an automation, use run_scenario.
- For questions about current state ("is the door open?", "temperature?"), use \
query_state and answer from the returned values; do not change anything.
- To control a robotic lawn mower (Navimow), use mower_command with the mower's \
deviceId from the catalog and one of START / STOP / PAUSE / RESUME / DOCK \
("start mowing" → START, "send the mower home" → DOCK).
- Keep spoken replies short and natural, in the same language the user used.'''

    /** Neutral tool definitions: {name, description, input_schema (JSON Schema)}. */
    static final List<Map> TOOLS = [
        [
            name        : CONTROL_ENTITY,
            description : 'Turn a peripheral, a whole zone, or a single port on/off or toggle it.',
            input_schema: [
                type      : 'object',
                properties: [
                    entityType: [type: 'string', enum: ['PERIPHERAL', 'ZONE', 'PORT'],
                                 description: 'PERIPHERAL for one device, ZONE for everything in an area, PORT for a raw port.'],
                    id        : [type: 'integer', description: 'The catalog id of the chosen entity.'],
                    action    : [type: 'string', enum: ['ON', 'OFF', 'TOGGLE']]
                ],
                required  : ['entityType', 'id', 'action']
            ]
        ],
        [
            name        : RUN_SCENARIO,
            description : 'Run a predefined automation/scenario by its job id from the catalog.',
            input_schema: [
                type      : 'object',
                properties: [jobId: [type: 'integer', description: 'The jobId of the scenario from the catalog.']],
                required  : ['jobId']
            ]
        ],
        [
            name        : QUERY_STATE,
            description : 'Read the current state/value of a peripheral, zone or port to answer a question.',
            input_schema: [
                type      : 'object',
                properties: [
                    entityType: [type: 'string', enum: ['PERIPHERAL', 'ZONE', 'PORT']],
                    id        : [type: 'integer']
                ],
                required  : ['entityType', 'id']
            ]
        ],
        [
            name        : MOWER_COMMAND,
            description : 'Control a Segway Navimow robotic lawn mower: start/stop/pause/resume mowing or send it back to the dock.',
            input_schema: [
                type      : 'object',
                properties: [
                    deviceId: [type: 'integer', description: 'The deviceId of the mower from the catalog (mowers list).'],
                    action  : [type: 'string', enum: ['START', 'STOP', 'PAUSE', 'RESUME', 'DOCK']]
                ],
                required  : ['deviceId', 'action']
            ]
        ]
    ]

    /** Deterministic catalog JSON (stable ordering) so the cached prefix stays cache-friendly. */
    static String catalogJson(Map catalog) {
        JsonOutput.toJson(catalog)
    }
}
