"""Model state: trains heads in the background and serves decisions.

Training data = sentences generated from the catalog (templates) + examples learned from
myHAB's feedback (the LLM's labels for real phrasings), the latter repeated `learned_weight`
times so real speech outweighs the templates.

Two triggers:
  catalog  a new catalog hash from myHAB; the new heads always go live (stale ids are worse)
  retrain  periodic, same catalog, new labels since the last training; the new heads go
           live only if they score no worse on the held-out set (NLU_HELDOUT, default
           <data>/heldout.json)

Previous heads keep serving while new ones train; the swap is atomic. Trained heads are
persisted under <data>/heads-<encoder>-<hash>.joblib and the newest is reloaded on start.
"""
import logging
import threading
import time
from pathlib import Path

import joblib

from .data import build_training
from .evaluate import accuracy, load_cases
from .heads import decide, fit_heads

log = logging.getLogger("voice_nlu")
KEEP_HEAD_FILES = 3
PROMOTION_TOLERANCE = 0.005


class NotReady(Exception):
    pass


class NluService:
    def __init__(self, profile, encoder, data_dir, feedback=None, learned_weight=3, cap_per_label=50,
                 heldout_path=None):
        self.profile = profile
        self.encoder = encoder
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.feedback = feedback
        self.learned_weight = learned_weight
        self.cap_per_label = cap_per_label
        self.heldout_path = Path(heldout_path) if heldout_path else self.data_dir / "heldout.json"
        self._lock = threading.Lock()
        self._state = None          # {hash, catalog, heads, trainedAt, rows, learned, labels, heldout}
        self._training = None       # catalog hash being trained
        self.last_error = None
        self.last_retrain = None    # outcome of the last periodic retrain

    # ------------------------------------------------------------------ persistence
    def _files(self):
        return sorted(self.data_dir.glob(f"heads-{self.profile.name}-*.joblib"), key=lambda p: p.stat().st_mtime)

    def load_latest(self):
        files = self._files()
        if not files:
            return False
        self._state = joblib.load(files[-1])
        log.info("loaded heads %s (catalog %s)", files[-1].name, self._state["hash"])
        return True

    def _persist(self, state):
        joblib.dump(state, self.data_dir / f"heads-{self.profile.name}-{state['hash'][:16]}.joblib")
        for old in self._files()[:-KEEP_HEAD_FILES]:
            old.unlink(missing_ok=True)

    # ------------------------------------------------------------------ training
    def submit_catalog(self, catalog_hash, catalog, wait=False):
        """Retrain for a new catalog; a hash already served or in training is a no-op."""
        with self._lock:
            if catalog_hash in (self.catalog_hash, self._training):
                return "unchanged"
            self._training = catalog_hash
        return self._run(catalog_hash, catalog, "catalog", wait)

    def retrain(self, wait=False):
        """Periodic retrain on the served catalog when new labels arrived since the last training."""
        state = self._state
        if state is None or self.feedback is None or "catalog" not in state:
            return "skipped"   # heads from before learning existed: the next catalog sync retrains
        if self.feedback.label_count() <= state.get("labels", 0):
            self.last_retrain = {"at": _now(), "result": "no new labels"}
            return "no new labels"
        with self._lock:
            if self._training is not None:
                return "busy"
            self._training = state["hash"]
        return self._run(state["hash"], state["catalog"], "retrain", wait)

    def _run(self, catalog_hash, catalog, reason, wait):
        worker = threading.Thread(target=self._train, args=(catalog_hash, catalog, reason), daemon=True)
        worker.start()
        if wait:
            worker.join()
        return "training"

    def _train(self, catalog_hash, catalog, reason):
        started = time.perf_counter()
        try:
            rows = build_training(catalog, augment=self.profile.augment)
            labels = self.feedback.label_count() if self.feedback else 0
            learned = self.feedback.learned_rows(catalog, self.cap_per_label) if self.feedback else []
            all_rows = rows + learned * self.learned_weight
            embeddings = self.encoder.encode([r[0] for r in all_rows])
            heads = fit_heads(all_rows, embeddings, balanced=self.profile.balanced)

            cases = load_cases(self.heldout_path)
            score = accuracy(lambda t: self._decide(t, heads), cases)
            current = self._state
            if reason == "retrain" and current is not None and score is not None:
                current_score = accuracy(lambda t: self._decide(t, current["heads"]), cases)
                if current_score is not None and score < current_score - PROMOTION_TOLERANCE:
                    # Remember the labels as seen, so the same set is not retried every period.
                    current["labels"] = labels
                    self.last_retrain = {"at": _now(), "result": "rejected", "learned": len(learned),
                                         "heldout": score, "currentHeldout": current_score}
                    log.warning("retrain rejected: held-out %.3f < current %.3f", score, current_score)
                    return

            state = {"hash": catalog_hash, "catalog": catalog, "heads": heads, "trainedAt": _now(),
                     "rows": len(all_rows), "learned": len(learned), "labels": labels,
                     "heldout": score, "encoder": self.profile.name}
            self._persist(state)
            with self._lock:
                self._state = state
                self.last_error = None
            if reason == "retrain":
                self.last_retrain = {"at": _now(), "result": "promoted", "learned": len(learned), "heldout": score}
            log.info("trained heads (%s) for catalog %s: %d rows (%d learned) in %.0fs, held-out %s",
                     reason, catalog_hash[:12], len(all_rows), len(learned), time.perf_counter() - started,
                     "n/a" if score is None else f"{score:.3f}")
        except Exception as ex:  # keep serving the previous heads
            log.exception("training (%s) failed for catalog %s", reason, catalog_hash)
            self.last_error = str(ex)
        finally:
            with self._lock:
                if self._training == catalog_hash:
                    self._training = None

    def _decide(self, text, heads):
        return decide(text, self.encoder.encode([text])[0], heads)[0]

    # ------------------------------------------------------------------ serving
    @property
    def catalog_hash(self):
        return self._state["hash"] if self._state else None

    def health(self):
        s = self._state or {}
        return {"ready": self._state is not None, "encoder": self.profile.name, "suggestedGate": self.profile.gate,
                "model": getattr(self.encoder, "model_file", None), "catalogHash": s.get("hash"),
                "trainedAt": s.get("trainedAt"), "rows": s.get("rows"), "learned": s.get("learned"),
                "heldout": s.get("heldout"), "training": self._training is not None,
                "lastRetrain": self.last_retrain, "lastError": self.last_error}

    def resolve(self, text):
        state = self._state
        if state is None:
            raise NotReady("no heads trained yet; PUT /catalog first")
        started = time.perf_counter()
        embedding = self.encoder.encode([text])[0]
        pred, confidence, top = decide(text, embedding, state["heads"])
        return {**pred, "confidence": round(confidence, 4), "catalogHash": state["hash"],
                "ms": round((time.perf_counter() - started) * 1000, 1), "top": top}


def _now():
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
