"""Pure parsing of a Tuya intercom picture reference.

The bridge publishes the raw value of a `pic` event DP on
`myhab/tuya/<code>/lastpic/state`. Two shapes are seen in the wild (both are the
DP value base64-encoded):

- **doorbell_pic (154):** the base64 decodes to a ready-to-fetch presigned S3 URL
  (`https://…?X-Amz-Expires=60&…`). No key — the object is usually a plain JPEG.
- **movement_detect_pic (115):** the base64 decodes to JSON
  `{"bucket": "...", "files": [["/path.jpeg", "<aes-key>"]]}`. The path must be
  turned into a URL via the Tuya Cloud, and the fetched bytes are AES-decrypted
  with the per-file key.

This module only parses; fetching, cloud presigning and decryption live in
`cloud.py`/`server.py` so the parsing stays trivially testable.
"""

import base64
import json
from dataclasses import dataclass


@dataclass
class PicRef:
    kind: str          # "url" | "bucket"
    url: str = None    # kind == "url"
    bucket: str = None  # kind == "bucket"
    path: str = None   # kind == "bucket"
    key: str = None    # AES key when the image is encrypted (may be None)


def parse_ref(raw):
    """Decode a lastpic DP payload into a PicRef, or None if unrecognized."""
    if not raw:
        return None
    text = raw.strip()
    # The payload is base64; fall back to treating it as already-decoded text.
    try:
        decoded = base64.b64decode(text).decode("utf-8", errors="strict")
    except Exception:
        decoded = text

    decoded = decoded.strip()
    if decoded.startswith("http://") or decoded.startswith("https://"):
        return PicRef(kind="url", url=decoded)
    if decoded.startswith("{"):
        try:
            obj = json.loads(decoded)
        except ValueError:
            return None
        files = obj.get("files") or []
        if not files or not files[0]:
            return None
        first = files[0]
        path = first[0] if len(first) > 0 else None
        key = first[1] if len(first) > 1 and first[1] else None
        if not path:
            return None
        return PicRef(kind="bucket", bucket=obj.get("bucket"), path=path, key=key)
    return None


def looks_like_jpeg(data):
    """True if the bytes start with the JPEG SOI marker."""
    return bool(data) and len(data) >= 2 and data[0] == 0xFF and data[1] == 0xD8
