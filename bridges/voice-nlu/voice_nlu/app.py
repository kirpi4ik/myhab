"""HTTP API for myhab's voice fast path.

    GET  /health   readiness, encoder, catalog hash of the served heads
    PUT  /catalog  {hash, catalog} -> 202, retrains in the background (no-op for a known hash)
    POST /resolve  {text, locale} -> decision {intent, target, action, scenario, mowerAction, confidence, ...}

Environment: NLU_ENCODER (e5-small | minilm | e5-base), NLU_MODELS_DIR (/models),
NLU_DATA_DIR (/data), NLU_THREADS (2), NLU_PORT (8090).
"""
import logging
import os

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .service import NluService, NotReady


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

    return app


def build_from_env() -> FastAPI:
    from .encoder import OnnxEncoder
    from .encoders import model_dir, profile

    prof = profile(os.environ.get("NLU_ENCODER", "e5-small"))
    data_dir = os.environ.get("NLU_DATA_DIR", "/data")
    encoder = OnnxEncoder(model_dir(prof.name, os.environ.get("NLU_MODELS_DIR", "/models"), data_dir),
                          prefix=prof.prefix, threads=int(os.environ.get("NLU_THREADS", "2")))
    service = NluService(prof, encoder, data_dir)
    service.load_latest()
    return create_app(service)


def main():
    import uvicorn

    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
    uvicorn.run(build_from_env(), host="0.0.0.0", port=int(os.environ.get("NLU_PORT", "8090")), access_log=False)


if __name__ == "__main__":
    main()
