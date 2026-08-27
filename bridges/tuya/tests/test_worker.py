from tuya_bridge.bridge import DeviceWorker


def _worker(dps):
    return DeviceWorker({"code": "x", "id": "i", "ip": "1.1.1.1", "key": "k",
                         "version": "3.3", "dps": dps}, "myhab/tuya", lambda *a: None)


def test_event_only_device_is_flagged():
    # A camera/doorbell with only event DPs won't answer status(); the worker
    # must know to listen for pushes instead of gating on a poll.
    w = _worker([{"dp": 154, "port_type": "event", "port_ref": "doorbell", "kind": "event"}])
    assert w.has_event_dps is True


def test_pollable_device_is_not_event_only():
    w = _worker([{"dp": 1, "port_type": "switch", "port_ref": "valve", "kind": "bool"}])
    assert w.has_event_dps is False
