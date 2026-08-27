import json

import pytest

from tuya_bridge import mapping


def test_bool_dp_to_payload():
    assert mapping.dp_to_payload("bool", True) == "ON"
    assert mapping.dp_to_payload("bool", False) == "OFF"
    assert mapping.dp_to_payload("bool", "true") == "ON"


def test_value_dp_to_payload_is_raw():
    assert mapping.dp_to_payload("value", 42) == "42"
    assert mapping.dp_to_payload("value", "auto") == "auto"


def test_payload_to_bool_dp():
    assert mapping.payload_to_dp("bool", "ON") is True
    assert mapping.payload_to_dp("bool", "off") is False
    assert mapping.payload_to_dp("bool", " on ") is True


def test_payload_to_bool_dp_rejects_garbage():
    with pytest.raises(ValueError):
        mapping.payload_to_dp("bool", "OPEN")


BASE = "myhab/tuya"


def test_topic_round_trip():
    topic = mapping.cmd_topic(BASE, "water_main_valve", "switch", "valve")
    assert topic == "myhab/tuya/water_main_valve/switch/valve/cmd"
    assert mapping.parse_cmd_topic(topic, BASE) == ("water_main_valve", "switch", "valve")


def test_parse_cmd_topic_rejects_foreign_topics():
    assert mapping.parse_cmd_topic("myhab/tuya/x/switch/valve/state", BASE) is None
    assert mapping.parse_cmd_topic("myhab/x/switch/valve/cmd", BASE) is None
    assert mapping.parse_cmd_topic("myhab/tuya/x/cmd", BASE) is None


def test_state_and_status_topics():
    assert mapping.state_topic(BASE, "v", "switch", "valve") == "myhab/tuya/v/switch/valve/state"
    assert mapping.status_topic(BASE, "v") == "myhab/tuya/v/status"
    assert mapping.cmd_subscription(BASE) == "myhab/tuya/+/+/+/cmd"


def test_event_dp_to_payload_is_raw():
    # An event DP (doorbell/motion) carries its raw reference through unchanged.
    ref = "eyJidWNrZXQiOiJ0eSJ9"
    assert mapping.dp_to_payload("event", ref) == ref


def test_notify_topic_uses_server_prefix_not_tuya_base():
    # NOTIFY is '<prefix>/<source>/notify' — one level up from the myhab/tuya base.
    assert mapping.notify_topic("myhab", "intercom") == "myhab/intercom/notify"
    assert "tuya" not in mapping.notify_topic("myhab", "intercom")


def test_notify_envelope_shape():
    body = json.loads(mapping.notify_envelope("Doorbell", "Someone rang", "INFO", "intercom.doorbell", 1))
    assert body == {
        "subject": "Doorbell",
        "message": "Someone rang",
        "level": "INFO",
        "dedupKey": "intercom.doorbell",
        "cooldown": 1,
    }


def test_lastpic_topic_dodges_read_and_status_patterns():
    # Two segments after the code -> matches neither TUYA read (4) nor status (2 ending 'status').
    t = mapping.lastpic_topic(BASE, "intercom")
    assert t == "myhab/tuya/intercom/lastpic/state"
    assert mapping.parse_cmd_topic(t, BASE) is None
