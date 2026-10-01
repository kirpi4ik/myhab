import pytest
from fastapi.testclient import TestClient

from voice_nlu.app import create_app
from voice_nlu.service import NluService, NotReady


@pytest.fixture
def service(profile, encoder, tmp_path):
    return NluService(profile, encoder, tmp_path)


def test_not_ready_before_catalog(service):
    assert service.health()["ready"] is False
    with pytest.raises(NotReady):
        service.resolve("turn on the kitchen light")


def test_catalog_then_resolve(service, catalog):
    assert service.submit_catalog("h1", catalog, wait=True) == "training"
    assert service.health()["ready"] and service.catalog_hash == "h1"
    d = service.resolve("turn off the kitchen light")
    assert d["intent"] == "control" and d["target"] == "P1" and d["action"] == "OFF"
    assert 0 < d["confidence"] <= 1 and d["catalogHash"] == "h1"


def test_known_hash_is_a_no_op(service, catalog):
    service.submit_catalog("h1", catalog, wait=True)
    assert service.submit_catalog("h1", catalog) == "unchanged"


def test_heads_survive_a_restart(profile, encoder, tmp_path, catalog):
    NluService(profile, encoder, tmp_path).submit_catalog("h1", catalog, wait=True)
    restarted = NluService(profile, encoder, tmp_path)
    assert restarted.load_latest() and restarted.catalog_hash == "h1"


def test_failed_training_keeps_previous_heads(service, catalog):
    service.submit_catalog("h1", catalog, wait=True)
    service.submit_catalog("h2", {"peripherals": "broken"}, wait=True)
    assert service.catalog_hash == "h1" and service.health()["lastError"]


def test_http_api(service, catalog):
    client = TestClient(create_app(service))
    assert client.post("/resolve", json={"text": "turn on the kitchen light"}).status_code == 503
    assert client.put("/catalog", json={"hash": "h1", "catalog": catalog}).status_code == 202
    service.submit_catalog("h1", catalog)  # already training or trained: no-op
    for _ in range(100):
        if client.get("/health").json()["ready"]:
            break
        import time
        time.sleep(0.05)
    r = client.post("/resolve", json={"text": "aprinde lumina bucătărie", "locale": "ro-RO"})
    assert r.status_code == 200 and r.json()["target"] in ("P1", "Z10") and r.json()["action"] == "ON"
    assert client.post("/resolve", json={"text": "  "}).status_code == 400
