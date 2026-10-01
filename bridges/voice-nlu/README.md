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
| `GET` | `/health` | → `{ready, encoder, suggestedGate, model, catalogHash, trainedAt, rows, training, lastError}` |
| `PUT` | `/catalog` | `{hash, catalog}` → `202 {status: training\|unchanged}` |
| `POST` | `/resolve` | `{text, locale}` → `{intent, target, action, scenario, mowerAction, confidence, catalogHash, ms, top}`, or `503` before the first training |

- `target` is `P<peripheralId>` or `Z<zoneId>`, and `scenario` is `S<jobId>`.
- `intent` is one of `control`, `scenario`, `query`, `mower`, `clarify` or `other`.
- `confidence` is the lowest confidence among the heads the intent needed.

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
| `NLU_DATA_DIR` | `/data` (trained heads; mount a volume) |
| `NLU_THREADS` | `2` (ONNX intra-op threads) |
| `NLU_PORT` | `8090` |

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
