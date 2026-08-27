"""Pure DP <-> MQTT mapping helpers.

The myHAB contract (see README.md): boolean DPs travel as literal ON/OFF —
myHAB compares port values to those strings for auditing, auto-off and the UI
toggle — while value DPs pass through as their raw string representation.

A third kind, ``event``, carries momentary signals (doorbell press, motion) that
have no terminal state. Instead of a retained port value it fans out to two
non-port topics: a ``<prefix>/<source>/notify`` envelope (an inbox message +
web-push, consumed by NotificationBridgeService) and, for image-bearing DPs, the
raw picture reference on ``<base>/<code>/lastpic/state`` for the cloud helper to
resolve into a JPEG.
"""

import json

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


def notify_topic(prefix, source):
    """'<prefix>/<source>/notify' — the myHAB NOTIFY channel.

    ``prefix`` is the server's mqtt.topic.prefix ('myhab'), NOT the bridge's
    'myhab/tuya' base: the NOTIFY pattern is exactly '<prefix>/<source>/notify'.
    """
    return f"{prefix}/{source}/notify"


def notify_envelope(subject, message, level, dedup_key, cooldown):
    """The JSON body NotificationBridgeService.onMqttNotification expects."""
    return json.dumps({
        "subject": subject,
        "message": message,
        "level": level,
        "dedupKey": dedup_key,
        "cooldown": cooldown,
    })


def lastpic_topic(base, code):
    """'<base>/<code>/lastpic/state' — the retained latest picture reference.

    Two segments after the code, so it matches neither the TUYA read
    ('<base>/w+/w+/w+/state') nor status ('<base>/w+/status') pattern — the
    server ignores it; only the intercom cloud helper subscribes.
    """
    return f"{base}/{code}/lastpic/state"
