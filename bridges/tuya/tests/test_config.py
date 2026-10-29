from tuya_bridge.bridge import CODE_RE


def test_code_allows_upper_lower_digits_underscore():
    # myHAB routes with \w+, so mixed-case codes (e.g. an intercom keyed 'TZ')
    # are valid — the bridge must not be stricter than the server.
    for ok in ("tz", "TZ", "water_main_valve", "Cam01", "INTERCOM_2"):
        assert CODE_RE.match(ok), ok


def test_code_rejects_hyphens_and_dots():
    # \w does not match '-' or '.', so these would publish but never route.
    for bad in ("water-valve", "cam.1", "", "a b"):
        assert not CODE_RE.match(bad), bad


def _load(tmp_path, device):
    import yaml
    from tuya_bridge.bridge import load_config
    path = tmp_path / "config.yaml"
    path.write_text(yaml.safe_dump({"devices": [dict({
        "code": "valve", "id": "i", "key": "k", "version": "3.4",
        "dps": [{"dp": 1, "port_type": "switch", "port_ref": "valve"}]}, **device)]}))
    return load_config(str(path))["devices"][0]


def test_missing_ip_defaults_to_auto_discovery(tmp_path):
    assert _load(tmp_path, {})["ip"] == "auto"


def test_static_ip_is_kept(tmp_path):
    assert _load(tmp_path, {"ip": "10.0.0.5"})["ip"] == "10.0.0.5"
