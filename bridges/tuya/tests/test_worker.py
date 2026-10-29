import pytest

from tuya_bridge import bridge
from tuya_bridge.bridge import DeviceWorker

VALVE = [{"dp": 1, "port_type": "switch", "port_ref": "valve", "kind": "bool"}]
DOORBELL = [{"dp": 154, "port_type": "event", "port_ref": "doorbell", "kind": "event"}]
UNREACHABLE = {"Error": "Network Error: Unable to Connect", "Err": "901", "Payload": None}


def _worker(dps, ip="1.1.1.1", published=None):
    publish = (lambda t, p, r: published.append((t, p, r))) if published is not None else (lambda *a: None)
    return DeviceWorker({"code": "x", "id": "i", "ip": ip, "key": "k",
                         "version": "3.3", "dps": dps}, "myhab/tuya", publish)


@pytest.fixture
def fake_device(monkeypatch):
    """Stands in for tinytuya.Device: status() returns what the test sets, and the
    first device call stops the worker so run() does exactly one connect attempt."""
    state = {"status": None, "address": None, "worker": None}

    class FakeDevice:
        def __init__(self, dev_id, address, key, version):
            state["address"] = address

        def set_socketPersistent(self, persistent):
            pass

        def status(self):
            state["worker"].stop()
            return state["status"]

        def receive(self):
            state["worker"].stop()
            return None

        def close(self):
            pass

    monkeypatch.setattr(bridge.tinytuya, "Device", FakeDevice)
    return state


def _run(fake_device, worker, status):
    fake_device.update(status=status, worker=worker)
    worker.run()


def test_event_only_device_is_flagged():
    # A camera/doorbell with only event DPs won't answer status(); the worker
    # must know to listen for pushes instead of gating on a poll.
    assert _worker(DOORBELL).has_event_dps is True


def test_pollable_device_is_not_event_only():
    assert _worker(VALVE).has_event_dps is False


def test_unreachable_device_publishes_offline_on_first_attempt(fake_device):
    # After a power cut the broker restores the last retained "online"; a bridge
    # that can't reach the device must overwrite it rather than stay silent.
    published = []
    _run(fake_device, _worker(VALVE, published=published), UNREACHABLE)
    assert ("myhab/tuya/x/status", "offline", True) in published
    assert ("myhab/tuya/x/status", "online", True) not in published


def test_unreachable_event_only_device_is_not_marked_online(fake_device):
    published = []
    _run(fake_device, _worker(DOORBELL, published=published), UNREACHABLE)
    assert ("myhab/tuya/x/status", "online", True) not in published
    assert ("myhab/tuya/x/status", "offline", True) in published


def test_reachable_event_only_device_goes_online_without_state(fake_device):
    # A reachable camera answers the DP query with nothing; it still listens.
    published = []
    _run(fake_device, _worker(DOORBELL, published=published), None)
    assert published[0] == ("myhab/tuya/x/status", "online", True)


def test_auto_ip_is_resolved_by_tinytuya_discovery(fake_device):
    _run(fake_device, _worker(VALVE, ip="auto"), UNREACHABLE)
    assert fake_device["address"] == "Auto"


def test_static_ip_is_passed_through(fake_device):
    _run(fake_device, _worker(VALVE, ip="10.0.0.5"), UNREACHABLE)
    assert fake_device["address"] == "10.0.0.5"
