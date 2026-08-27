"""Tuya-local <-> MQTT bridge for myHAB.

One MQTT connection (QoS 1, retained state, LWT on the bridge status topic);
one thread per Tuya device holding a persistent local-protocol socket that
receives DP pushes, sends heartbeats and periodically refreshes full status.
Topic/payload contract: README.md in this directory.
"""

import argparse
import logging
import queue
import re
import signal
import sys
import threading
import time

import paho.mqtt.client as mqtt
import tinytuya
import yaml

from . import mapping

log = logging.getLogger("tuya_bridge")

# myHAB matches topic segments with \w+, so codes/refs may be [A-Za-z0-9_]
# (upper or lower). Hyphens are excluded — \w does not match '-', so a hyphenated
# code would publish fine but never route on the server.
CODE_RE = re.compile(r"^\w+$")
STATUS_REFRESH_SEC = 60
HEARTBEAT_SEC = 9
RECONNECT_MIN_SEC = 5
RECONNECT_MAX_SEC = 120
# The camera repeats an event DP a few times per press; collapse the burst.
EVENT_DEBOUNCE_SEC = 2


def load_config(path):
    with open(path, "r", encoding="utf-8") as fh:
        cfg = yaml.safe_load(fh)

    mqtt_cfg = cfg.get("mqtt") or {}
    mqtt_cfg.setdefault("host", "localhost")
    mqtt_cfg.setdefault("port", 1883)
    mqtt_cfg.setdefault("username", "")
    mqtt_cfg.setdefault("password", "")
    mqtt_cfg.setdefault("base_topic", "myhab/tuya")
    mqtt_cfg.setdefault("bridge_code", "tuya_bridge")

    devices = cfg.get("devices") or []
    if not devices:
        raise ValueError("config has no devices")
    for dev in devices:
        for key in ("code", "id", "key", "ip", "version"):
            if not dev.get(key):
                raise ValueError(f"device entry missing '{key}': {dev.get('code', dev)}")
        if not CODE_RE.match(dev["code"]):
            raise ValueError(f"device code must be [A-Za-z0-9_]+ (no hyphens): {dev['code']!r}")
        dps = dev.get("dps") or []
        if not dps:
            raise ValueError(f"device {dev['code']} has no dps mapping")
        for dp in dps:
            for key in ("dp", "port_type", "port_ref"):
                if not dp.get(key):
                    raise ValueError(f"dps entry of {dev['code']} missing '{key}'")
            dp.setdefault("kind", "bool")
            if not CODE_RE.match(str(dp["port_ref"])) or not CODE_RE.match(str(dp["port_type"])):
                raise ValueError(f"port_type/port_ref must be [A-Za-z0-9_]+ in {dev['code']}")
            if dp["kind"] == "event":
                notify = dp.get("notify") or {}
                if not notify.get("source") or not notify.get("subject"):
                    raise ValueError(f"event dp {dp['dp']} of {dev['code']} needs notify.source and notify.subject")
                notify.setdefault("message", notify["subject"])
                notify.setdefault("level", "INFO")
                notify.setdefault("dedup_key", f"{notify['source']}.{dev['code']}.{dp['port_ref']}")
                notify.setdefault("cooldown", 1)
                dp["notify"] = notify
    return {"mqtt": mqtt_cfg, "devices": devices}


class DeviceWorker(threading.Thread):
    """Holds the persistent Tuya-local socket for one device."""

    def __init__(self, dev_cfg, base_topic, publish):
        super().__init__(name=f"tuya-{dev_cfg['code']}", daemon=True)
        self.cfg = dev_cfg
        self.code = dev_cfg["code"]
        self.base = base_topic
        # The notify channel lives at the server prefix ('myhab'), one level up
        # from the bridge's 'myhab/tuya' base.
        self.prefix = base_topic.split("/", 1)[0]
        self.publish = publish  # (topic, payload, retain) -> None
        self.commands = queue.Queue()
        self.stop_event = threading.Event()
        self.online = False
        # dp index (as str) -> dps config entry
        self.dp_map = {str(dp["dp"]): dp for dp in dev_cfg["dps"]}
        self._event_last = {}  # dp index -> monotonic ts of last emit (debounce)

    def command(self, port_type, port_ref, payload):
        for dp in self.cfg["dps"]:
            if dp["port_type"] == port_type and str(dp["port_ref"]) == port_ref:
                try:
                    value = mapping.payload_to_dp(dp["kind"], payload)
                except ValueError as err:
                    log.warning("%s: dropping command on %s/%s: %s", self.code, port_type, port_ref, err)
                    return
                self.commands.put((dp, value))
                return
        log.warning("%s: no dps mapping for %s/%s — command ignored", self.code, port_type, port_ref)

    def stop(self):
        self.stop_event.set()

    def _set_online(self, online):
        if online != self.online:
            self.online = online
            self.publish(mapping.status_topic(self.base, self.code), "online" if online else "offline", True)
            log.info("%s: %s", self.code, "online" if online else "offline")

    def _publish_dps(self, dps):
        for index, raw in (dps or {}).items():
            dp = self.dp_map.get(str(index))
            if dp is None:
                continue  # unmapped DP — visible with a scan, deliberately unpublished
            if dp["kind"] == "event":
                self._emit_event(dp, raw)
                continue
            topic = mapping.state_topic(self.base, self.code, dp["port_type"], str(dp["port_ref"]))
            self.publish(topic, mapping.dp_to_payload(dp["kind"], raw), True)

    def _emit_event(self, dp, raw):
        """A momentary DP (doorbell/motion): fire a notify, and for a picture DP
        publish the raw reference for the cloud helper to resolve into a JPEG."""
        idx = str(dp["dp"])
        now = time.monotonic()
        if now - self._event_last.get(idx, 0.0) < EVENT_DEBOUNCE_SEC:
            return
        self._event_last[idx] = now
        n = dp["notify"]
        self.publish(mapping.notify_topic(self.prefix, n["source"]),
                     mapping.notify_envelope(n["subject"], n["message"], n["level"],
                                             n["dedup_key"], n["cooldown"]),
                     False)
        log.info("%s: event dp %s -> notify %s", self.code, idx, n["source"])
        if dp.get("pic"):
            self.publish(mapping.lastpic_topic(self.base, self.code), str(raw), True)

    def run(self):
        backoff = RECONNECT_MIN_SEC
        while not self.stop_event.is_set():
            device = None
            try:
                device = tinytuya.Device(self.cfg["id"], self.cfg["ip"], self.cfg["key"],
                                         version=float(self.cfg["version"]))
                device.set_socketPersistent(True)

                status = device.status()
                if not status or "dps" not in status:
                    raise ConnectionError(f"initial status failed: {status}")
                self._set_online(True)
                backoff = RECONNECT_MIN_SEC
                self._publish_dps(status["dps"])

                last_heartbeat = time.monotonic()
                last_refresh = time.monotonic()
                while not self.stop_event.is_set():
                    # Commands first: confirmed state comes back as a DP push
                    # (or the periodic refresh), which is the echo myHAB's
                    # pending-action correlation and auto-off arming rely on.
                    try:
                        dp, value = self.commands.get(timeout=0.5)
                        result = device.set_value(int(dp["dp"]), value, nowait=False)
                        if result and result.get("Error"):
                            raise ConnectionError(f"set_value failed: {result}")
                        if result and "dps" in result:
                            self._publish_dps(result["dps"])
                        else:
                            self._publish_dps(device.status().get("dps"))
                    except queue.Empty:
                        pass

                    data = device.receive()
                    if data and "dps" in data:
                        self._publish_dps(data["dps"])
                    if data and data.get("Error"):
                        raise ConnectionError(f"receive error: {data}")

                    now = time.monotonic()
                    if now - last_heartbeat >= HEARTBEAT_SEC:
                        device.heartbeat(nowait=True)
                        last_heartbeat = now
                    if now - last_refresh >= STATUS_REFRESH_SEC:
                        status = device.status()
                        if status and "dps" in status:
                            self._publish_dps(status["dps"])
                        last_refresh = now
            except Exception as err:  # noqa: BLE001 — any socket/protocol failure means reconnect
                log.warning("%s: connection lost (%s); retrying in %ss", self.code, err, backoff)
                self._set_online(False)
                if self.stop_event.wait(backoff):
                    break
                backoff = min(backoff * 2, RECONNECT_MAX_SEC)
            finally:
                if device is not None:
                    try:
                        device.close()
                    except Exception:  # noqa: BLE001
                        pass
        self._set_online(False)


class Bridge:
    def __init__(self, cfg):
        self.cfg = cfg
        self.base = cfg["mqtt"]["base_topic"]
        self.bridge_code = cfg["mqtt"]["bridge_code"]
        self.client = mqtt.Client(client_id=f"myhab-{self.bridge_code}", clean_session=False)
        if cfg["mqtt"]["username"]:
            self.client.username_pw_set(cfg["mqtt"]["username"], cfg["mqtt"]["password"] or None)
        self.client.will_set(mapping.status_topic(self.base, self.bridge_code),
                             "offline", qos=1, retain=True)
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message
        self.workers = {
            dev["code"]: DeviceWorker(dev, self.base, self.publish)
            for dev in cfg["devices"]
        }

    def publish(self, topic, payload, retain):
        self.client.publish(topic, payload, qos=1, retain=retain)
        log.debug("-> %s %s%s", topic, payload, " (retained)" if retain else "")

    def _on_connect(self, client, userdata, flags, rc):
        log.info("MQTT connected (rc=%s)", rc)
        if rc != 0:
            # paho still calls on_connect on a refusal — subscribing/publishing here
            # would silently no-op against the socket the broker is about to close.
            log.error("MQTT connect refused: %s (rc=%s) — check host/port/username/password",
                     mqtt.connack_string(rc), rc)
            return
        log.info("MQTT connected")
        client.subscribe(mapping.cmd_subscription(self.base), qos=1)
        self.publish(mapping.status_topic(self.base, self.bridge_code), "online", True)

    def _on_message(self, client, userdata, msg):
        parsed = mapping.parse_cmd_topic(msg.topic, self.base)
        if parsed is None:
            return
        code, port_type, port_ref = parsed
        worker = self.workers.get(code)
        if worker is None:
            log.warning("command for unknown device %s ignored", code)
            return
        payload = msg.payload.decode("utf-8", errors="replace")
        log.info("<- %s %s", msg.topic, payload)
        worker.command(port_type, port_ref, payload)

    def run(self):
        self.client.connect(self.cfg["mqtt"]["host"], int(self.cfg["mqtt"]["port"]), keepalive=30)
        self.client.loop_start()
        for worker in self.workers.values():
            worker.start()

        stop = threading.Event()

        def shutdown(signum, frame):
            log.info("signal %s — shutting down", signum)
            stop.set()

        signal.signal(signal.SIGTERM, shutdown)
        signal.signal(signal.SIGINT, shutdown)
        stop.wait()

        for worker in self.workers.values():
            worker.stop()
        for worker in self.workers.values():
            worker.join(timeout=5)
        self.publish(mapping.status_topic(self.base, self.bridge_code), "offline", True)
        self.client.loop_stop()
        self.client.disconnect()


def main():
    parser = argparse.ArgumentParser(description="Tuya-local to MQTT bridge for myHAB")
    parser.add_argument("--config", default="/config/config.yaml")
    parser.add_argument("--log-level", default="INFO")
    args = parser.parse_args()

    logging.basicConfig(level=args.log_level.upper(),
                        format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    try:
        cfg = load_config(args.config)
    except (OSError, ValueError, yaml.YAMLError) as err:
        log.error("invalid config %s: %s", args.config, err)
        sys.exit(1)
    Bridge(cfg).run()


if __name__ == "__main__":
    main()