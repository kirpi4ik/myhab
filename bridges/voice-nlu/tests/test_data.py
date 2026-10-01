from voice_nlu.data import build_rows, build_training


def test_rows_cover_every_entity_and_intent(catalog):
    rows = build_rows(catalog)
    targets = {r[2] for r in rows}
    assert {"P1", "P2", "P3", "Z10"} <= targets
    assert {r[1] for r in rows} == {"control", "query", "scenario", "mower", "clarify", "other"}
    assert {r[4] for r in rows if r[1] == "scenario"} == {"S20"}


def test_sensors_are_query_only(catalog):
    assert {r[1] for r in build_rows(catalog) if r[2] == "P3"} == {"query"}


def test_openable_devices_get_open_close_verbs(catalog):
    texts = {r[0] for r in build_rows(catalog) if r[2] == "P2"}
    assert "open the garden valve" in texts and "close the garden valve" in texts
    assert "light up the garden valve" not in texts


def test_no_mower_rows_without_mowers(catalog):
    rows = build_rows({**catalog, "mowers": []})
    assert all(r[1] != "mower" for r in rows)


def test_augment_adds_accent_free_copies(catalog):
    plain = build_training(catalog, augment=False)
    augmented = build_training(catalog, augment=True)
    assert len(augmented) > len(plain)
    assert ("aprinde lumina bucatarie",) + ("control", "P1", "ON", "none", "none") in augmented
