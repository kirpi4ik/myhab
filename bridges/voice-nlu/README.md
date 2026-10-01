# myHAB voice NLU

The local first stage of the voice assistant's intent cascade (local NLU → Jev → Claude).

It resolves a transcript to one decision in tens of milliseconds, on CPU and offline. The decision is an intent, a target device or zone, an on/off/toggle action, a scenario or a mower command, together with a confidence. myHAB acts on the decision when the confidence clears its gate; otherwise it hands the transcript to the next stage. The measurements behind this design are in [`docs/VOICE_PERFORMANCE_OPTIONS.md`](../../docs/VOICE_PERFORMANCE_OPTIONS.md).

## How it works

- **Encoder:** a frozen multilingual sentence encoder, exported to int8 ONNX. No PyTorch is needed at runtime.
- **Classifier heads:** logistic-regression heads for intent, target, action, scenario and mower command.
- **Training data:** the heads are trained on sentences generated from **myHAB's voice catalog**, using EN/RO/RU templates over device and zone names and their `feature.voice.alias` aliases.
- **Action:** a deterministic EN/RO/RU verb list (`voice_nlu/lexicon.py`) sets the on/off/toggle action whenever it recognises the verb.
- **Retraining:** myHAB sends the catalog whenever its hash changes (`NluCatalogSyncJob`). The sidecar retrains in the background, which takes a few minutes. Until the new heads are ready it keeps serving the previous ones, and it persists the trained heads under `/data`.

## API

| Method | Path | Body → response |
|---|---|---|
| `GET` | `/health` | → `{ready, encoder, suggestedGate, model, catalogHash, trainedAt, rows, learned, heldout, training, lastRetrain, lastError}` |
| `PUT` | `/catalog` | `{hash, catalog}` → `202 {status: training\|unchanged}` |
| `POST` | `/resolve` | `{text, locale}` → `{intent, target, action, scenario, mowerAction, confidence, catalogHash, ms, top}`, or `503` before the first training |
| `POST` | `/feedback` | one record per voice command (see "Learning") → `202 {status, labelled}` |

- `target` is `P<peripheralId>` or `Z<zoneId>`, and `scenario` is `S<jobId>`.
- `intent` is one of `control`, `scenario`, `query`, `mower`, `clarify` or `other`.
- `confidence` is the lowest confidence among the heads the intent needed.

## Learning

The sidecar improves from real commands, with the LLM as the teacher.

- **Feedback records.** After every voice command myHAB posts a record to `/feedback` (switch it off with `feature.voice.nlu.feedback=false`). The record holds the transcript, the locale, each stage's decision, the stage that resolved the command, and a `label`.
- **Labels.** myHAB sets a label only when the **LLM** resolved the command with exactly one tool call that worked, on a catalog entity. Fast-path decisions are logged but never labelled, so the model cannot reinforce its own mistakes. Answers to a clarifying question are never labelled either, because they only make sense together with the earlier turn.
- **Storage.** Records are kept under `/data/log/feedback-YYYY-MM.jsonl` for `NLU_LOG_RETENTION_DAYS`, and the transcripts never leave the installation.
- **Retraining.** Every retrain adds the labelled phrasings to the template sentences:
  - deduplicated by text, with the latest label winning
  - at most 50 per label
  - only for entities still in the catalog
  - repeated `NLU_LEARNED_WEIGHT` times, so real speech outweighs the templates
- **When retrains happen:**
  - **Catalog change** (`PUT /catalog`): the new heads always go live.
  - **Periodic retrain:** every `NLU_RETRAIN_HOURS`, when new labels have arrived.
- **Held-out set.** Write a set of commands that never went into the templates, the verb list or the aliases, in the `tools/voice-eval/data/cases.json` format. It names your devices, so it is installation data: keep it with the deployment and mount it read-only, then point `NLU_HELDOUT` at it (for example `./voice-nlu/heldout.json:/config/heldout.json:ro` with `NLU_HELDOUT=/config/heldout.json`). Without `NLU_HELDOUT`, the sidecar looks for `/data/heldout.json`.
  - A periodic retrain then replaces the current heads only if it scores no worse on the held-out set.
  - Every training reports its held-out accuracy in `/health` (`heldout`, and `lastRetrain` for the periodic ones).

**If accuracy plateaus**, fine-tune the encoder offline:

1. Train jointly on target × action labels, using the templates plus the real phrasings from `/data/log`. Training on target labels alone is what made the evaluated SetFit run worse.
2. Export it with `export_models.py`.
3. Place it under `/data/models/<encoder-name>/`, where it overrides the baked-in model.
4. Keep it only if the held-out score improves.

## Encoders

Set the encoder with `NLU_ENCODER`. All three are baked into the image. Switching means changing the variable and restarting; the sidecar then retrains for the current catalog.

| `NLU_ENCODER` | Model | Size (int8) | Suggested gate | Measured (dev CPU, int8) |
|---|---|---|---|---|
| `e5-small` (default) | multilingual-e5-small | 113 MB | 0.6 | 85.9% fully correct; 9 ms p50; 63% of commands answered locally in the cascade |
| `minilm` | paraphrase-multilingual-MiniLM-L12-v2 | 113 MB | 0.9 | 85.9% fully correct; 8 ms p50; 38% answered locally |
| `e5-base` | multilingual-e5-base | 265 MB | 0.6 | 91.9% fully correct; 30 ms p50; 55% answered locally |

For how the default was chosen, see `docs/VOICE_PERFORMANCE_OPTIONS.md` ("Encoder decision").

The suggested gate is reported in `/health`. Set it in myHAB as `feature.voice.nlu.gate`.

A model exported to `/data/models/<name>/` (`model_quantized.onnx` + `tokenizer.json`) takes precedence over the baked-in model of the same name. That is how a locally fine-tuned model is deployed.

## Environment

| Variable | Default |
|---|---|
| `NLU_ENCODER` | `e5-small` |
| `NLU_MODELS_DIR` | `/models` |
| `NLU_DATA_DIR` | `/data` (trained heads and the feedback log; mount a volume) |
| `NLU_THREADS` | `2` (ONNX intra-op threads) |
| `NLU_PORT` | `8090` |
| `NLU_RETRAIN_HOURS` | `24` (periodic retrain with new labels; `0` = off) |
| `NLU_LOG_RETENTION_DAYS` | `180` (feedback log retention) |
| `NLU_LEARNED_WEIGHT` | `3` (how many times each learned phrasing counts) |
| `NLU_HELDOUT` | `<NLU_DATA_DIR>/heldout.json` (held-out cases that gate the periodic retrain) |

## Development

```bash
python -m venv .venv-export
.venv-export/Scripts/python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
.venv-export/Scripts/python -m pip install "optimum[onnxruntime]" -r requirements.txt pytest httpx
.venv-export/Scripts/python -m pytest -q                      # stub encoder, no models needed
.venv-export/Scripts/python export_models.py --out models     # int8 ONNX for all encoders
NLU_MODELS_DIR=models NLU_DATA_DIR=data .venv-export/Scripts/python -m voice_nlu.app
```

To score accuracy and latency on the evaluation cases, use `tools/voice-eval/eval_sidecar.py`, in-process or with `--url`.

## Image

`docker build -t kirpi4ik/myhab-voice-nlu bridges/voice-nlu` builds three stages:

1. Exports the models.
2. Runs pytest.
3. Builds the slim runtime image.

CI pushes the image next to the main image, the same way as `bridges/tuya`.
