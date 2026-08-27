"""Intercom cloud helper.

Backend-only HTTP service (never exposed to the browser — the Grails
IntercomController proxies it under JWT). It:

- subscribes to `myhab/tuya/<code>/lastpic/state` and keeps the latest doorbell/
  motion picture reference per device;
- serves `GET /snapshot.jpg?device=<code>` — the last **event** image (resolved
  from that ref: fetch the presigned URL, AES-decrypt if needed), or, with
  `?live=1` or when no event image is available, a fresh Tuya-Cloud frame grab;
- serves `GET /stream?device=<code>` — `{ "hls": "<url>", "expires": <ts> }`.

With no Tuya-Cloud credentials configured, live grab and stream degrade to
empty/204 so the widget shows a placeholder — the event image still works
(doorbell) without the cloud.
"""

import argparse
import json
import logging
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

import paho.mqtt.client as mqtt
import requests
import yaml

from . import refs
from .cloud import TuyaCloud

log = logging.getLogger("intercom_cloud")

LASTPIC_SUFFIX = "/lastpic/state"
CACHE_TTL_SEC = 10


def load_config(path):
    with open(path, "r", encoding="utf-8") as fh:
        cfg = yaml.safe_load(fh) or {}
    m = cfg.setdefault("mqtt", {})
    m.setdefault("host", "localhost")
    m.setdefault("port", 1883)
    m.setdefault("username", "")
    m.setdefault("password", "")
    m.setdefault("base_topic", "myhab/tuya")
    cfg.setdefault("http", {}).setdefault("port", 8095)
    cfg.setdefault("tuya", {})
    cfg.setdefault("devices", {})  # myHAB device code -> tuya device id
    cfg.setdefault("ffmpeg", "ffmpeg")
    # Seconds of stream to read before keeping the last frame — long enough to get
    # past Tuya's black "connecting..." splash.
    cfg.setdefault("snapshot_grab_seconds", 8)
    return cfg


class State:
    """Latest picture ref per device code + a short-lived resolved-JPEG cache."""

    def __init__(self):
        self._lock = threading.Lock()
        self._lastpic = {}          # code -> raw ref payload
        self._cache = {}            # code -> (ts, jpeg_bytes)

    def set_lastpic(self, code, raw):
        with self._lock:
            self._lastpic[code] = raw
            self._cache.pop(code, None)

    def get_lastpic(self, code):
        with self._lock:
            return self._lastpic.get(code)

    def cached(self, code):
        with self._lock:
            hit = self._cache.get(code)
        if hit and time.monotonic() - hit[0] < CACHE_TTL_SEC:
            return hit[1]
        return None

    def store(self, code, jpeg):
        with self._lock:
            self._cache[code] = (time.monotonic(), jpeg)


def decrypt_jpeg(data, key):
    """AES-128-ECB decrypt a Tuya detect image (best-effort)."""
    try:
        from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
        cipher = Cipher(algorithms.AES(key.encode("utf-8")), modes.ECB())
        dec = cipher.decryptor()
        out = dec.update(data) + dec.finalize()
        # strip PKCS7 padding if present
        if out and 1 <= out[-1] <= 16:
            out = out[:-out[-1]]
        return out
    except Exception as err:  # noqa: BLE001
        log.warning("image decrypt failed: %s", err)
        return None


class Helper:
    def __init__(self, cfg):
        self.cfg = cfg
        self.base = cfg["mqtt"]["base_topic"]
        self.devices = cfg["devices"]  # code -> tuya id
        self.ffmpeg = cfg["ffmpeg"]
        self.grab_seconds = int(cfg["snapshot_grab_seconds"])
        self.state = State()
        t = cfg["tuya"]
        self.cloud = TuyaCloud(t.get("access_id"), t.get("access_secret"), t.get("region"))
        self.client = mqtt.Client(client_id="myhab-intercom-cloud", clean_session=True)
        if cfg["mqtt"]["username"]:
            self.client.username_pw_set(cfg["mqtt"]["username"], cfg["mqtt"]["password"] or None)
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message

    # -- MQTT --------------------------------------------------------------
    def _on_connect(self, client, userdata, flags, rc):
        if rc != 0:
            log.error("MQTT connect refused rc=%s", rc)
            return
        topic = f"{self.base}/+/lastpic/state"
        client.subscribe(topic, qos=1)
        log.info("subscribed %s", topic)

    def _on_message(self, client, userdata, msg):
        if not msg.topic.endswith(LASTPIC_SUFFIX):
            return
        code = msg.topic[len(self.base) + 1:-len(LASTPIC_SUFFIX)]
        self.state.set_lastpic(code, msg.payload.decode("utf-8", errors="replace"))
        log.info("lastpic updated for %s", code)

    # -- image resolution --------------------------------------------------
    def event_jpeg(self, code):
        raw = self.state.get_lastpic(code)
        ref = refs.parse_ref(raw) if raw else None
        if not ref or ref.kind != "url":
            return None  # bucket refs need cloud presign; fall back to live grab
        try:
            data = requests.get(ref.url, timeout=8).content
        except requests.RequestException as err:
            log.warning("event image fetch failed for %s: %s", code, err)
            return None
        if refs.looks_like_jpeg(data):
            return data
        if ref.key:
            dec = decrypt_jpeg(data, ref.key)
            if dec and refs.looks_like_jpeg(dec):
                return dec
        return None

    def snapshot(self, code, live):
        cached = self.state.cached(code)
        if cached and not live:
            return cached
        jpeg = None
        if not live:
            jpeg = self.event_jpeg(code)
        if jpeg is None:
            tuya_id = self.devices.get(code)
            if tuya_id:
                jpeg = self.cloud.frame_jpeg(tuya_id, ffmpeg=self.ffmpeg, grab_seconds=self.grab_seconds)
        if jpeg:
            self.state.store(code, jpeg)
        return jpeg

    def stream(self, code):
        tuya_id = self.devices.get(code)
        if not tuya_id:
            return None
        url = self.cloud.hls_url(tuya_id)
        if not url:
            return None
        return {"hls": url, "expires": int(time.time()) + 60}

    # -- run ---------------------------------------------------------------
    def run(self):
        self.client.connect(self.cfg["mqtt"]["host"], int(self.cfg["mqtt"]["port"]), keepalive=30)
        self.client.loop_start()
        httpd = ThreadingHTTPServer(("0.0.0.0", int(self.cfg["http"]["port"])), _handler(self))
        log.info("http listening on :%s", self.cfg["http"]["port"])
        try:
            httpd.serve_forever()
        finally:
            self.client.loop_stop()
            self.client.disconnect()


def _handler(helper):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # quiet; we log meaningful events ourselves

        def _q(self):
            return parse_qs(urlparse(self.path).query)

        def do_GET(self):
            path = urlparse(self.path).path
            q = self._q()
            code = (q.get("device") or [None])[0]
            if path == "/snapshot.jpg":
                live = (q.get("live") or ["0"])[0] in ("1", "true")
                jpeg = helper.snapshot(code, live) if code else None
                if not jpeg:
                    self.send_response(204)
                    self.end_headers()
                    return
                self.send_response(200)
                self.send_header("Content-Type", "image/jpeg")
                self.send_header("Cache-Control", "no-store")
                self.send_header("Content-Length", str(len(jpeg)))
                self.end_headers()
                self.wfile.write(jpeg)
            elif path == "/stream":
                data = helper.stream(code) if code else None
                if not data:
                    self.send_response(204)
                    self.end_headers()
                    return
                body = json.dumps(data).encode()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            elif path == "/healthz":
                self.send_response(200)
                self.end_headers()
                self.wfile.write(b"ok")
            else:
                self.send_response(404)
                self.end_headers()

    return Handler


def main():
    parser = argparse.ArgumentParser(description="Intercom Tuya-Cloud media helper for myHAB")
    parser.add_argument("--config", default="/config/config.yaml")
    parser.add_argument("--log-level", default="INFO")
    args = parser.parse_args()
    logging.basicConfig(level=args.log_level.upper(),
                        format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    try:
        cfg = load_config(args.config)
    except (OSError, yaml.YAMLError) as err:
        log.error("invalid config %s: %s", args.config, err)
        sys.exit(1)
    Helper(cfg).run()


if __name__ == "__main__":
    main()
