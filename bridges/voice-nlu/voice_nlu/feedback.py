"""Decision log and the training examples learned from it.

myHAB posts one record per voice command to /feedback. A record carries a `label` only
when the LLM resolved the command with a single successful tool call: the LLM acts as
the teacher. Decisions the fast path executed itself are logged but never labelled, so
the model cannot reinforce its own mistakes.

Records go to <data>/log/feedback-YYYY-MM.jsonl; months older than the retention are
deleted on write.
"""
import json
import threading
from datetime import datetime, timedelta, timezone
from pathlib import Path

from .lexicon import strip_accents

ACTIONS = {"ON", "OFF", "TOGGLE"}
MOWER_ACTIONS = {"START", "STOP", "PAUSE", "RESUME", "DOCK"}


class FeedbackLog:
    def __init__(self, data_dir, retention_days=180):
        self.dir = Path(data_dir) / "log"
        self.dir.mkdir(parents=True, exist_ok=True)
        self.retention_days = retention_days
        self._lock = threading.Lock()

    def append(self, record):
        now = datetime.now(timezone.utc)
        record = {"ts": now.strftime("%Y-%m-%dT%H:%M:%SZ"), **record}
        line = json.dumps(record, ensure_ascii=False) + "\n"
        with self._lock:
            with (self.dir / f"feedback-{now:%Y-%m}.jsonl").open("a", encoding="utf-8") as f:
                f.write(line)
            self._prune(now)

    def _prune(self, now):
        cutoff = (now - timedelta(days=self.retention_days)).strftime("%Y-%m")
        for f in self.dir.glob("feedback-*.jsonl"):
            if f.stem.removeprefix("feedback-") < cutoff:
                f.unlink(missing_ok=True)

    def records(self):
        for f in sorted(self.dir.glob("feedback-*.jsonl")):
            with f.open(encoding="utf-8") as fh:
                for line in fh:
                    line = line.strip()
                    if line:
                        try:
                            yield json.loads(line)
                        except json.JSONDecodeError:
                            continue

    def learned_rows(self, catalog, cap_per_label=50):
        """Labelled records as training rows, valid for `catalog`; the latest label per text wins."""
        keys = {f"P{p['id']}" for p in catalog.get("peripherals") or []} | \
               {f"Z{z['id']}" for z in catalog.get("zones") or []}
        scenarios = {f"S{s['jobId']}" for s in catalog.get("scenarios") or []}
        has_mower = bool(catalog.get("mowers"))

        latest = {}
        for rec in self.records():
            label, text = rec.get("label"), (rec.get("text") or "").strip()
            row = _row(text, label, keys, scenarios, has_mower) if label and text else None
            if row:
                norm = " ".join(strip_accents(text.lower()).split())
                latest.pop(norm, None)        # re-insert so dict order = recency
                latest[norm] = row

        per_label, rows = {}, []
        for row in reversed(list(latest.values())):   # newest first
            n = per_label.get(row[1:], 0)
            if n < cap_per_label:
                per_label[row[1:]] = n + 1
                rows.append(row)
        return rows

    def label_count(self):
        return sum(1 for r in self.records() if r.get("label"))


def _row(text, label, keys, scenarios, has_mower):
    intent = label.get("intent")
    target, action = label.get("target"), label.get("action")
    if intent == "control" and target in keys and action in ACTIONS:
        return (text, "control", target, action, "none", "none")
    if intent == "query" and target in keys:
        return (text, "query", target, "none", "none", "none")
    if intent == "scenario" and label.get("scenario") in scenarios:
        return (text, "scenario", "none", "none", label["scenario"], "none")
    if intent == "mower" and has_mower and label.get("mowerAction") in MOWER_ACTIONS:
        return (text, "mower", "none", "none", "none", label["mowerAction"])
    return None
