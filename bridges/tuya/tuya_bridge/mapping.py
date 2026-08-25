"""Pure DP <-> MQTT mapping helpers.

The myHAB contract (see README.md): boolean DPs travel as literal ON/OFF —
myHAB compares port values to those strings for auditing, auto-off and the UI
toggle — while value DPs pass through as their raw string representation.
"""

ON = "ON"
OFF = "OFF"


def dp_to_payload(kind, raw):
    """Tuya DP value -> MQTT state payload."""
    if kind == "bool":
        return ON if raw in (True, "true", "True", 1, "1") else OFF
    return str(raw)


def payload_to_dp(kind, payload):
    """MQTT command payload -> Tuya DP value.

    Raises ValueError for a bool DP payload that is not ON/OFF so the caller
    can log-and-drop instead of actuating on garbage.
    """
    text = payload.strip().upper()
    if kind == "bool":
        if text == ON:
            return True
        if text == OFF:
            return False
        raise ValueError(f"expected ON/OFF for a bool DP, got {payload!r}")
    return payload.strip()


def state_topic(base, code, port_type, port_ref):
    return f"{base}/{code}/{port_type}/{port_ref}/state"


def cmd_topic(base, code, port_type, port_ref):
    return f"{base}/{code}/{port_type}/{port_ref}/cmd"


def status_topic(base, code):
    return f"{base}/{code}/status"


def cmd_subscription(base):
    return f"{base}/+/+/+/cmd"


def parse_cmd_topic(topic, base):
    """'<base>/<code>/<port_type>/<port_ref>/cmd' -> (code, port_type, port_ref) or None.

    The base may itself contain slashes (the default is 'myhab/tuya').
    """
    prefix = base + "/"
    if not topic.startswith(prefix):
        return None
    parts = topic[len(prefix):].split("/")
    if len(parts) != 4 or parts[3] != "cmd":
        return None
    return parts[0], parts[1], parts[2]
