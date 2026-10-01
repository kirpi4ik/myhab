"""Model state: trains heads for a catalog in the background and serves decisions.

Previous heads keep serving while new ones train; the swap is atomic. Trained heads are
persisted under <data>/heads-<encoder>-<hash>.joblib and the newest is reloaded on start.
"""
import logging
import threading
import time
from pathlib import Path

import joblib

from .data import build_training
from .heads import decide, fit_heads

log = logging.getLogger("voice_nlu")
KEEP_HEAD_FILES = 3


class NotReady(Exception):
    pass


class NluService:
    def __init__(self, profile, encoder, data_dir):
        self.profile = profile
        self.encoder = encoder
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self._lock = threading.Lock()
        self._state = None          # {hash, heads, trainedAt, rows}
        self._training_hash = None
        self.last_error = None

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
            if catalog_hash in (self.catalog_hash, self._training_hash):
                return "unchanged"
            self._training_hash = catalog_hash
        worker = threading.Thread(target=self._train, args=(catalog_hash, catalog), daemon=True)
        worker.start()
        if wait:
            worker.join()
        return "training"

    def _train(self, catalog_hash, catalog):
        started = time.perf_counter()
        try:
            rows = build_training(catalog, augment=self.profile.augment)
            embeddings = self.encoder.encode([r[0] for r in rows])
            heads = fit_heads(rows, embeddings, balanced=self.profile.balanced)
            state = {"hash": catalog_hash, "heads": heads, "trainedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                     "rows": len(rows), "encoder": self.profile.name}
            self._persist(state)
            with self._lock:
                self._state = state
                self.last_error = None
            log.info("trained heads for catalog %s: %d rows in %.0fs", catalog_hash, len(rows), time.perf_counter() - started)
        except Exception as ex:  # keep serving the previous heads
            log.exception("training failed for catalog %s", catalog_hash)
            self.last_error = str(ex)
        finally:
            with self._lock:
                if self._training_hash == catalog_hash:
                    self._training_hash = None

    # ------------------------------------------------------------------ serving
    @property
    def catalog_hash(self):
        return self._state["hash"] if self._state else None

    def health(self):
        s = self._state or {}
        return {"ready": self._state is not None, "encoder": self.profile.name, "suggestedGate": self.profile.gate,
                "model": getattr(self.encoder, "model_file", None), "catalogHash": s.get("hash"),
                "trainedAt": s.get("trainedAt"), "rows": s.get("rows"), "training": self._training_hash is not None,
                "lastError": self.last_error}

    def resolve(self, text):
        state = self._state
        if state is None:
            raise NotReady("no heads trained yet; PUT /catalog first")
        started = time.perf_counter()
        embedding = self.encoder.encode([text])[0]
        pred, confidence, top = decide(text, embedding, state["heads"])
        return {**pred, "confidence": round(confidence, 4), "catalogHash": state["hash"],
                "ms": round((time.perf_counter() - started) * 1000, 1), "top": top}
