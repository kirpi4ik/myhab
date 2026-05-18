package org.myhab.services.navimow

/**
 * Commands the Segway Navimow REST API accepts on
 * {@code POST /openapi/smarthome/sendCommands}.
 *
 * <p>Each enum member carries the wire-level {@code command} string and the
 * optional {@code params} map that Segway's cloud expects. The mapping mirrors
 * the SDK's {@code mower_sdk/api.py:async_send_command} — start and stop share
 * the same wire command (StartStop) with {@code on:true|false}; pause and
 * resume share PauseUnpause similarly.</p>
 *
 * <p>To send a command, the caller looks up {@code wireCommand} and
 * {@code wireParams} and POSTs:</p>
 * <pre>
 * {
 *   "commands": [{
 *     "devices": [{"id": "&lt;segwayDeviceId&gt;"}],
 *     "execution": {
 *       "command": "&lt;wireCommand&gt;",
 *       "params":  {&lt;wireParams&gt;}     // omitted when wireParams == null
 *     }
 *   }]
 * }
 * </pre>
 */
enum MowerCommand {
    START ('action.devices.commands.StartStop',     [on: true]),
    STOP  ('action.devices.commands.StartStop',     [on: false]),
    PAUSE ('action.devices.commands.PauseUnpause',  [on: false]),
    RESUME('action.devices.commands.PauseUnpause',  [on: true]),
    DOCK  ('action.devices.commands.Dock',          null)

    final String wireCommand
    final Map<String, Object> wireParams

    MowerCommand(String wireCommand, Map<String, Object> wireParams) {
        this.wireCommand = wireCommand
        this.wireParams = wireParams
    }

    /** Case-insensitive parse used by the GraphQL mutation. Returns null on miss. */
    static MowerCommand fromName(String name) {
        if (!name) return null
        return values().find { it.name().equalsIgnoreCase(name.trim()) }
    }
}
