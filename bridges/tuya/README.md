# myHAB Tuya bridge

A small Python bridge that controls Tuya WiFi devices (e.g. the ATLO-V1-TUYA
water valve) over the **Tuya local protocol** (TCP 6668 on the device itself,
via [tinytuya](https://github.com/jasonacox/tinytuya)) and exposes them on the
MQTT broker using myHAB's `TUYA` topic dialect. Control is fully local:
sub-second, no internet dependency at runtime.

## Topic contract (all QoS 1)

| Purpose | Topic | Payload | Retained | Direction |
|---|---|---|---|---|
| Port state | `myhab/tuya/<code>/<portType>/<portRef>/state` | `ON`\|`OFF` (bool DPs), raw value otherwise | yes | bridge → myHAB |
| Command | `myhab/tuya/<code>/<portType>/<portRef>/cmd` | `ON`\|`OFF` | no | myHAB → bridge |
| Device availability | `myhab/tuya/<code>/status` | `online`\|`offline` | yes | bridge → myHAB |
| Bridge availability | `myhab/tuya/<bridge_code>/status` | `online`\|`offline` (LWT) | yes | broker/bridge |

Everything lives under the server's standard `myhab` prefix (its `mqtt.topic.prefix`
config), in a `/tuya/` sub-namespace so the four-segment topics cannot collide with
the ESP dialect's three-segment ones. Anything publishing under `myhab/tuya/` must
speak this contract — in
particular, boolean state is always the literal `ON`/`OFF` (myHAB compares
port values to those strings for the UI toggle, auditing and auto-off).
Device codes and port refs are `[a-z0-9_]+` — **no hyphens**; the server-side
regexes use `\w+` and silently drop hyphenated codes.

Video streams (Tuya cameras/intercoms) do **not** flow over Tuya-local DPs and
are out of this bridge's scope — expect no video topics here.

## Getting device credentials (one-time)

The valve needs nothing installed; its stock firmware runs the local protocol.
You only need its **device id + local key**, held by Tuya's cloud:

1. `pip install tinytuya`
2. `python -m tinytuya wizard` — creates/links a free Tuya IoT developer
   account with your Smart Life account and prints every device's id and key.
3. `python -m tinytuya scan` — finds the device's LAN IP, protocol version and
   the live DP table (confirm which DP is the open/close boolean; DP 1 on most
   valves).
4. Give the device a **static DHCP lease** so the configured IP stays valid.

Caveats:
- **Re-pairing regenerates the local key.** If the device is removed/re-added
  in Smart Life, re-run the wizard and update the config.
- Tuya devices accept **one local TCP connection at a time**. This bridge holds
  it; the Smart Life app then transparently falls back to its cloud channel,
  so both keep working.
- Local keys are **installation secrets** — they belong in the operator's
  private config, never in this repository.

## Configuration

`config.example.yaml` is a **template with neutral defaults** — it is the only
config this public repo ships. Your real `config.yaml` (device ids, local keys,
LAN IPs, broker credentials) is installation data: keep it in your private ops
repo and mount it read-only at `/config/config.yaml` (or pass `--config`),
the same way the myHAB image takes its external `config/application.yml`. Per DP you map a Tuya data point
to a myHAB port: `port_type` (the `DevicePort.type`, lowercased) and
`port_ref` (the `DevicePort.internalRef`), plus `kind`:

- `bool` — translated to/from `ON`/`OFF` (switches, valves)
- `value` — raw passthrough (counters, temperatures); published as state only

## Running

```bash
pip install -r requirements.txt
python -m tuya_bridge.bridge --config ./config.yaml
```

Or via Docker (the image build runs the test suite):

```bash
docker build -t myhab-tuya-bridge .
docker run -v $(pwd)/config.yaml:/config/config.yaml:ro myhab-tuya-bridge
```

CI publishes the image as `kirpi4ik/myhab-tuya-bridge`.

## myHAB side

1. Nothing to change in `mqtt.topics`: the standard `myhab/#` subscription
   already covers the `myhab/tuya/` sub-namespace.
2. Create the Device (code = the bridge `code`, model `TUYA`), a `SWITCH`
   port with `internalRef` = the bridge `port_ref`, and a peripheral in the
   `VALVE` category connected to it. Set `key.on.timeout` on the peripheral
   for auto-close.
3. Optionally create a Device row for `bridge_code` (model `TUYA`, no ports)
   so bridge availability shows in the admin UI via its LWT.

## Tests

```bash
pip install -r requirements.txt pytest
pytest
```
