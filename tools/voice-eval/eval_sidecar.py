"""Evaluate the voice-nlu sidecar (bridges/voice-nlu) on data/cases.json.

Two modes, same JSONL output as the other runners (score with Summarize-Results.ps1):

  in-process (default)   loads NluService with the int8 ONNX export; Ms = model time only
      .venv/Scripts/python eval_sidecar.py --encoder e5-small --models ../../bridges/voice-nlu/models
  HTTP                   PUT /catalog, wait for /health ready, POST /resolve; Ms = round trip
      .venv/Scripts/python eval_sidecar.py --url http://localhost:8090

--cases selects another case file, e.g. data/cases.heldout.json.
"""
import argparse
import hashlib
import json
import sys
import tempfile
import time
import urllib.request
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).parent
DATA = ROOT / "data"
RESULTS = ROOT / "results"
BRIDGE = ROOT.parent.parent / "bridges" / "voice-nlu"


def http(method, url, body=None, timeout=30):
    data = None if body is None else json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, method=method, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", help="running sidecar; omit for in-process")
    ap.add_argument("--encoder", default="minilm")
    ap.add_argument("--models", default=str(BRIDGE / "models"))
    ap.add_argument("--cases", default=str(DATA / "cases.json"))
    ap.add_argument("--repeat", type=int, default=5)
    ap.add_argument("--langs", default="en,ro,ru")
    args = ap.parse_args()

    catalog_text = (DATA / "catalog.json").read_text(encoding="utf-8")
    catalog = json.loads(catalog_text)
    catalog_hash = hashlib.sha256(catalog_text.encode("utf-8")).hexdigest()
    cases = json.loads(Path(args.cases).read_text(encoding="utf-8"))

    t0 = time.perf_counter()
    if args.url:
        http("PUT", f"{args.url}/catalog", {"hash": catalog_hash, "catalog": catalog})
        while not (h := http("GET", f"{args.url}/health"))["ready"] or h["catalogHash"] != catalog_hash:
            time.sleep(2)
        encoder_name = h["encoder"]

        def resolve(text):
            return http("POST", f"{args.url}/resolve", {"text": text})
    else:
        sys.path.insert(0, str(BRIDGE))
        from voice_nlu.encoder import OnnxEncoder
        from voice_nlu.encoders import model_dir, profile
        from voice_nlu.service import NluService

        prof = profile(args.encoder)
        encoder = OnnxEncoder(model_dir(prof.name, args.models, tempfile.gettempdir()), prefix=prof.prefix)
        service = NluService(prof, encoder, tempfile.mkdtemp(prefix="voice-nlu-"))
        service.submit_catalog(catalog_hash, catalog, wait=True)
        if not service.health()["ready"]:
            sys.exit(f"training failed: {service.health()['lastError']}")
        encoder_name = prof.name
        resolve = service.resolve
    print(f"{encoder_name}: ready in {time.perf_counter() - t0:.0f}s")

    langs = args.langs.split(",")
    work = [(f"{c['id']}.{l}", l, c["text"][l]) for c in cases for l in langs if c["text"].get(l)]
    resolve(work[0][2])  # warm-up

    RESULTS.mkdir(exist_ok=True)
    mode = "http" if args.url else "local"
    out_file = RESULTS / f"sidecar-{encoder_name}-{mode}-{datetime.now():%Y%m%d-%H%M%S}.jsonl"
    keys = ("intent", "target", "action", "scenario", "mowerAction")
    with out_file.open("w", encoding="utf-8") as f:
        for rep in range(1, args.repeat + 1):
            for case_id, lang, text in work:
                t = time.perf_counter()
                d = resolve(text)
                ms = (time.perf_counter() - t) * 1000
                f.write(json.dumps({"CaseId": case_id, "Lang": lang, "Repeat": rep, "Ok": True, "Ms": round(ms, 1),
                                    "Pred": {k: d.get(k) for k in keys}, "Confidence": d["confidence"],
                                    "Top": d.get("top")}, ensure_ascii=False) + "\n")
    print(f"results: {out_file}")


if __name__ == "__main__":
    main()
