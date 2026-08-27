import base64
import json

from intercom_cloud import refs


def b64(s):
    return base64.b64encode(s.encode()).decode()


def test_parse_direct_url_doorbell():
    url = "https://ty-eu-storage30-pic.s3.eu-central-1.amazonaws.com/x/detect/1.jpeg?X-Amz-Expires=60"
    ref = refs.parse_ref(b64(url))
    assert ref.kind == "url"
    assert ref.url == url
    assert ref.key is None


def test_parse_bucket_motion_with_key():
    payload = json.dumps({"v": "3.0", "bucket": "ty-eu-storage30-pic",
                          "files": [["/x/detect/2.jpeg", "d22dc445aa24dbee"]]})
    ref = refs.parse_ref(b64(payload))
    assert ref.kind == "bucket"
    assert ref.bucket == "ty-eu-storage30-pic"
    assert ref.path == "/x/detect/2.jpeg"
    assert ref.key == "d22dc445aa24dbee"


def test_parse_bucket_without_key():
    payload = json.dumps({"bucket": "b", "files": [["/p.jpeg", ""]]})
    ref = refs.parse_ref(b64(payload))
    assert ref.kind == "bucket"
    assert ref.key is None


def test_parse_handles_already_decoded_text():
    url = "https://example.com/x.jpeg"
    assert refs.parse_ref(url).url == url


def test_parse_rejects_garbage_and_empty():
    assert refs.parse_ref("") is None
    assert refs.parse_ref(b64("not json not url")) is None
    assert refs.parse_ref(b64("{bad json")) is None


def test_looks_like_jpeg():
    assert refs.looks_like_jpeg(b"\xff\xd8\xff\xe0stuff")
    assert not refs.looks_like_jpeg(b"\x89PNG")
    assert not refs.looks_like_jpeg(b"")
