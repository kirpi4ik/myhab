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
