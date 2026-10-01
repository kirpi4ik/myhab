"""HTTP API for myhab's voice fast path.

    GET  /health    readiness, encoder, catalog hash, learned examples, held-out score
    PUT  /catalog   {hash, catalog} -> 202, retrains in the background (no-op for a known hash)
    POST /resolve   {text, locale} -> decision {intent, target, action, scenario, mowerAction, confidence, ...}
    POST /feedback  one record per voice command (decisions, resolving stage, the LLM's label) -> 202

Environment: NLU_ENCODER (e5-small | minilm | e5-base), NLU_MODELS_DIR (/models),
NLU_DATA_DIR (/data), NLU_THREADS (2), NLU_PORT (8090), NLU_RETRAIN_HOURS (24, 0 = off),
NLU_LOG_RETENTION_DAYS (180), NLU_LEARNED_WEIGHT (3), NLU_HELDOUT (<data>/heldout.json).
"""
import logging
import os
import threading

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .feedback import FeedbackLog
from .service import NluService, NotReady

log = logging.getLogger("voice_nlu")


class CatalogBody(BaseModel):
    hash: str
    catalog: dict


class ResolveBody(BaseModel):
    text: str
    locale: str | None = None


def create_app(service: NluService) -> FastAPI:
    app = FastAPI(title="myHAB voice NLU", docs_url=None, redoc_url=None)

    @app.get("/health")
    def health():
        return service.health()

    @app.put("/catalog", status_code=202)
    def put_catalog(body: CatalogBody):
        return {"status": service.submit_catalog(body.hash, body.catalog), "catalogHash": body.hash}

    @app.post("/resolve")
    def resolve(body: ResolveBody):
        if not body.text.strip():
            raise HTTPException(400, "empty text")
        try:
            return service.resolve(body.text)
        except NotReady as ex:
            return JSONResponse(status_code=503, content={"error": str(ex)})

    @app.post("/feedback", status_code=202)
    def feedback(record: dict):
        if service.feedback is None:
            raise HTTPException(404, "feedback logging is not configured")
        if not str(record.get("text") or "").strip():
            raise HTTPException(400, "empty text")
        service.feedback.append(record)
        return {"status": "logged", "labelled": bool(record.get("label"))}

    return app


def start_retrain_loop(service: NluService, hours: float):
    """Retrain every `hours` when new labels arrived; daemon thread, stops with the process."""
    if hours <= 0:
        return None
    stop = threading.Event()

    def loop():
        while not stop.wait(hours * 3600):
            try:
                log.info("periodic retrain: %s", service.retrain())
            except Exception:
                log.exception("periodic retrain failed")

    threading.Thread(target=loop, name="nlu-retrain", daemon=True).start()
    return stop


def build_from_env() -> FastAPI:
    from .encoder import OnnxEncoder
    from .encoders import model_dir, profile

    prof = profile(os.environ.get("NLU_ENCODER", "e5-small"))
    data_dir = os.environ.get("NLU_DATA_DIR", "/data")
    encoder = OnnxEncoder(model_dir(prof.name, os.environ.get("NLU_MODELS_DIR", "/models"), data_dir),
                          prefix=prof.prefix, threads=int(os.environ.get("NLU_THREADS", "2")))
    feedback = FeedbackLog(data_dir, retention_days=int(os.environ.get("NLU_LOG_RETENTION_DAYS", "180")))
    service = NluService(prof, encoder, data_dir, feedback=feedback,
                         learned_weight=int(os.environ.get("NLU_LEARNED_WEIGHT", "3")),
                         heldout_path=os.environ.get("NLU_HELDOUT") or None)
    service.load_latest()
    start_retrain_loop(service, float(os.environ.get("NLU_RETRAIN_HOURS", "24")))
    return create_app(service)


def main():
    import uvicorn

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    uvicorn.run(build_from_env(), host="0.0.0.0", port=int(os.environ.get("NLU_PORT", "8090")), access_log=False)


if __name__ == "__main__":
    main()
