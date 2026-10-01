import json

import pytest
from fastapi.testclient import TestClient

from voice_nlu.app import create_app
from voice_nlu.evaluate import correct
from voice_nlu.feedback import FeedbackLog
from voice_nlu.service import NluService

ODD = "make the place where we cook bright"   # a phrasing no template produces


def label(intent, target=None, action=None, scenario=None, mowerAction=None):
    return {"intent": intent, "target": target, "action": action, "scenario": scenario, "mowerAction": mowerAction}


@pytest.fixture
def log(tmp_path):
    return FeedbackLog(tmp_path)


@pytest.fixture
def service(profile, encoder, tmp_path, log):
    return NluService(profile, encoder, tmp_path, feedback=log, learned_weight=3)


def test_learned_rows_keep_valid_labels_only(log, catalog):
    log.append({"text": "kitchen please", "label": label("control", "P1", "ON")})
    log.append({"text": "the ghost", "label": label("control", "P999", "ON")})        # not in the catalog
    log.append({"text": "no label here", "label": None})
    log.append({"text": "run evening", "label": label("scenario", scenario="S20")})
    log.append({"text": "weird", "label": label("control", "P1", "DIM")})             # unknown action
    rows = log.learned_rows(catalog)
    assert sorted(rows) == sorted([("kitchen please", "control", "P1", "ON", "none", "none"),
                                   ("run evening", "scenario", "none", "none", "S20", "none")])


def test_latest_label_per_text_wins_ignoring_accents_and_case(log, catalog):
    log.append({"text": "Stinge lumina bucătărie", "label": label("control", "P1", "ON")})
    log.append({"text": "stinge lumina bucatarie", "label": label("control", "P1", "OFF")})
    assert log.learned_rows(catalog) == [("stinge lumina bucatarie", "control", "P1", "OFF", "none", "none")]


def test_cap_per_label(log, catalog):
    for i in range(5):
        log.append({"text": f"kitchen light on number {i}", "label": label("control", "P1", "ON")})
    assert len(log.learned_rows(catalog, cap_per_label=2)) == 2


def test_old_months_are_pruned(tmp_path):
    log = FeedbackLog(tmp_path, retention_days=30)
    old = log.dir / "feedback-2000-01.jsonl"
    old.write_text(json.dumps({"text": "x", "label": None}) + "\n", encoding="utf-8")
    log.append({"text": "y"})
    assert not old.exists() and log.label_count() == 0


def test_a_learned_phrasing_is_resolved_after_retraining(service, log, catalog):
    service.submit_catalog("h1", catalog, wait=True)
    assert service.retrain(wait=True) == "no new labels"

    log.append({"text": ODD, "label": label("control", "P1", "ON")})
    assert service.retrain(wait=True) == "training"
    d = service.resolve(ODD)
    assert (d["intent"], d["target"], d["action"]) == ("control", "P1", "ON")
    assert service.health()["learned"] == 1
    assert service.health()["lastRetrain"]["result"] == "promoted"


def test_a_retrain_that_hurts_the_held_out_set_is_rejected(service, log, catalog, tmp_path):
    (tmp_path / "heldout.json").write_text(json.dumps([
        {"id": "kitchen-off", "accept": [{"intent": "control", "target": "P1", "action": "OFF"}],
         "text": {"en": "turn off the kitchen light"}}]), encoding="utf-8")
    service.submit_catalog("h1", catalog, wait=True)
    assert service.health()["heldout"] == 1.0

    # Poisoned labels: the held-out phrasing taught as the garden valve.
    for _ in range(5):
        log.append({"text": "turn off the kitchen light", "label": label("control", "P2", "OFF")})
    service.retrain(wait=True)
    assert service.health()["lastRetrain"]["result"] == "rejected"
    assert service.resolve("turn off the kitchen light")["target"] == "P1"
    assert service.retrain(wait=True) == "no new labels"   # the rejected set is not retried


def test_held_out_set_from_a_configured_path(profile, encoder, tmp_path, catalog):
    path = tmp_path / "config" / "heldout.json"
    path.parent.mkdir()
    path.write_text(json.dumps([
        {"id": "kitchen-off", "accept": [{"intent": "control", "target": "P1", "action": "OFF"}],
         "text": {"en": "turn off the kitchen light"}}]), encoding="utf-8")
    service = NluService(profile, encoder, tmp_path / "data", heldout_path=path)
    service.submit_catalog("h1", catalog, wait=True)
    assert service.health()["heldout"] == 1.0


def test_a_new_catalog_always_goes_live(service, log, catalog, tmp_path):
    (tmp_path / "heldout.json").write_text(json.dumps([
        {"id": "x", "accept": [{"intent": "control", "target": "P1", "action": "OFF"}],
         "text": {"en": "turn off the kitchen light"}}]), encoding="utf-8")
    service.submit_catalog("h1", catalog, wait=True)
    service.submit_catalog("h2", {**catalog, "scenarios": []}, wait=True)
    assert service.catalog_hash == "h2"


def test_feedback_endpoint(service, log):
    client = TestClient(create_app(service))
    r = client.post("/feedback", json={"text": "turn on the kitchen light", "resolvedBy": "llm",
                                       "label": label("control", "P1", "ON")})
    assert r.status_code == 202 and r.json()["labelled"] is True
    assert client.post("/feedback", json={"text": " "}).status_code == 400
    assert log.label_count() == 1


def test_correct_treats_clarify_and_other_alike():
    assert correct({"intent": "other"}, [{"intent": "clarify"}])
    assert not correct({"intent": "control", "target": "P1", "action": "ON"}, [{"intent": "clarify"}])
