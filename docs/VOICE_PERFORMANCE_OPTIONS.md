# Voice assistant: performance and offline options

Status (2026-10):

- **Implemented:**
  - Phase 1: the `bridges/voice-nlu` sidecar.
  - Phase 2: the fast path in `VoiceCommandService`.
  - Phase 3: the Jev stage (`JevStage`). With it, the full local NLU → Jev → Claude cascade is in place; each stage is off by default and has a shadow mode.
  - Phase 4: the training loop. A decision log is kept, with LLM-labelled phrasings learned on retrain, daily retrains, and a held-out promotion gate.
- **Docs:** `docs/VOICE_ASSISTANT.md` §1.9 and `bridges/voice-nlu/README.md`.
- **Not built:** an offline encoder fine-tune. It is documented as the next step if accuracy plateaus.
The evaluation tooling (Jev, Claude, Laya, BERT/SetFit and the cascade simulator) is in [`tools/voice-eval/`](../tools/voice-eval/).

## 1. Where the time goes today

| Stage | Where | Cost |
|---|---|---|
| Wake word | Android, microWakeWord (TFLite-Micro) | local, negligible |
| Speech-to-text | Android `SpeechRecognizer` (on-device first, online fallback); browser Web Speech API | a fixed 1200 ms end-of-speech silence hint |
| Intent | `VoiceCommandService.handleTranscript`: a tool-use loop of **blocking** LLM round-trips (Claude Haiku 4.5, up to 6) | at least **2 calls** for a plain "turn on X": one for `tool_use`, one to phrase the reply |
| Text-to-speech | Google Cloud TTS, called **after** the loop | one more serial round-trip |

Other observations:

- The catalog is rebuilt from the database on every request.
- The catalog block uses the default 5-minute prompt cache. Home commands are usually further apart than that, so most calls pay for a cache write rather than a cache read.
- The server does no timing of its own. The only figure is on Android: the debug log line `timing: stt=…ms server=…ms`.
- Matching has no local fast path or fallback. Aliases (`feature.voice.alias`) are only read by the LLM.

**Conclusion:** the pipeline's structure costs more than the choice of model: the second LLM call, the serial TTS call and the silence timeout. Fix those before swapping models.

## 2. Options for the intent step

| Option | Latency (i5, 8 GB, no GPU) | Offline | EN / RO / RU | Integration |
|---|---|---|---|---|
| Deterministic matcher: normalize text, detect the action from keywords, fuzzy-match names and aliases (Jaro-Winkler, diacritics folded) | < 5 ms | yes | yes | plain Groovy, no dependencies |
| **BERT embeddings + classifier heads** (frozen multilingual MiniLM / e5, plus a verb lexicon) | **20–40 ms, measured** on a laptop CPU | yes | yes | 0.5–1 GB RAM; heads re-fitted when the catalog changes; see section 5 |
| **Jev** (TypeSafe "System One" model) | 70–500 ms (vendor claim), **measured by `tools/voice-eval`** | no, API only | "English best, others: test" | HTTP; confidence per answer |
| Claude Haiku 4.5 (current) | ~0.7–1.5 s per call (estimate) | no | yes | existing path; handles conversation and clarification |
| Small local LLM (Qwen3-1.7B via llama-server, JSON schema with an enum of ids) | 1–5 s with the catalog in the KV cache, far slower without it | yes | RO weaker at this size | ~1.5 GB RAM, tight next to the JVM and Postgres |
| Hassil / Speech-to-Phrase (template grammars) | very fast | yes | yes | Python/Kaldi only; high cost to maintain templates |

### Jev in brief

- **API:** `POST https://api.typesafe.ai/v1/systemone` with `{state, model, questions}`.
- **Questions:** typed choice, score or boolean ("noul") questions, all answered in parallel with calibrated probabilities. A choice question allows up to 255 options.
- **Output:** no free text. Jev cannot phrase a reply or ask a clarifying question, so replies would have to come from templates.
- **Pricing:** $0.042 per million input tokens; output is free.
- **Limits:** 64k context, 40 requests per second.
- **Maturity:** announced 2026-09-15, currently in early access. Weights are closed, so it can't run locally.

Jev is a natural fit for picking one device and one action from our catalog. Whether it's good enough is what the evaluation below measures.

## 3. Speech-to-text on the server (ONNX)

**What:** [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) (Apache-2.0) ships a JVM jar with native libraries for linux-x64. The best multilingual model for it is [NVIDIA Parakeet TDT 0.6B v3](https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3):

- covers 25 European languages, including RO, RU and EN
- detects the language automatically
- INT8 ONNX build is about 1 GB

Other models:

- **Whisper-small int8** is an alternative.
- **Vosk** has no Romanian model.

**Advantages over the current setup:**

- The same recognition quality on every client: Android, browser, and future satellite microphones. Whether Android offers Romanian offline depends on the device.
- Recognition can be biased toward our device and zone names (hotwords), which is where most misrecognitions happen.
- A server-side voice-activity detector can end the utterance after a shorter silence than today's 1200 ms.
- Audio can be kept to tune accuracy later.

**Disadvantages:**

- Not faster: Android recognition is already on-device, and uploading audio adds a hop.
- Needs about 1–1.5 GB more RAM on the server.
- Needs a new audio-upload API; the server accepts only text today.

**Verdict:** worth doing for accuracy, Romanian and satellite microphones, not for latency. Parakeet's speed on this CPU is not yet measured.

## 4. Recommendation

1. **Instrument.** Record per-stage timings in `VoiceCommandService`: catalog, each LLM call, tools, TTS.
2. **Quick wins that keep Claude:**
   - When the only tool calls were successful `control_entity` / `run_scenario` calls, skip the second LLM call and speak a templated confirmation (RO/EN).
   - Cache `buildCatalog()`.
   - Use a 1-hour cache TTL on the catalog block.
   - Cache the TTS audio for fixed phrases.
   - Shorten the Android silence hint.
3. **Put a cascade in front of the LLM: local BERT → Jev → Claude** (measured in section 5, "Cascade"). Each stage acts only on a single actionable decision above its confidence gate. Anything else falls through, unchanged, to the Claude tool loop.
4. **Degrade gracefully offline.** When the cloud is unreachable, answer from the local BERT stage.
5. **Later:** server-side speech-to-text (sherpa-onnx + Parakeet v3) once satellite microphones or Romanian accuracy become the priority.

## 5. Jev evaluation

### How to run

Requirements: Windows PowerShell 5.1+ and `psql` on PATH. The local dev database must hold a copy of the installation's data.

```powershell
cd tools/voice-eval
.\Export-VoiceCatalog.ps1                  # -> data/catalog.json (git-ignored)
# data/cases.json: {id, accept:[{intent,target,action,scenario,mowerAction}], text:{en,ro,ru}}
$env:JEV_API_KEY = '...'                   # or set it as a user/system environment variable
.\Invoke-JevEval.ps1 -Max 3 -Repeat 1      # smoke test
.\Invoke-JevEval.ps1                       # full run, 5 repeats, en/ro/ru
$env:ANTHROPIC_API_KEY = '...'
.\Invoke-ClaudeBaseline.ps1                # same cases, production prompt, first turn only
```

The local models run in a Python venv (git-ignored; CPU PyTorch, `laya`, `setfit`; the models are downloaded from Hugging Face on first use):

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
.\.venv\Scripts\python -m pip install laya setfit
.\.venv\Scripts\python eval_laya.py --mode shortlist          # Laya multilingual, top-20 shortlist
.\.venv\Scripts\python eval_setfit.py --finetune-steps 0 --no-augment             # MiniLM, own-row heads + lexicon
.\.venv\Scripts\python eval_setfit.py --finetune-steps 0 --balanced --base intfloat/multilingual-e5-base
.\.venv\Scripts\python eval_setfit.py                          # SetFit fine-tune + heads (not recommended)
# ablation flags: --heads v1 (old heads), --no-augment, --no-lexicon, --balanced
.\Summarize-Results.ps1 -File results\<run>.jsonl             # same scoring as the Jev/Claude runners
.\Simulate-Cascade.ps1 -Bert results\<bert>.jsonl -Jev results\<jev>.jsonl -Claude results\<claude>.jsonl
```

Each run writes to `results/` (git-ignored):

- a raw log, `*.jsonl`
- a summary, `*-summary.md`, covering latency percentiles per language, accuracy (intent, target, action, fully correct), consistency across repeats, calibration by confidence band (Jev only), cost, and a list of distinct misses with Jev's top probabilities

**How Jev is asked.** Each case is a single request. The transcript is sent as `state`, together with parallel choice questions:

- `intent`: control, scenario, query, mower, clarify, other
- `target`: every peripheral and zone, plus `none`
- `action`: ON, OFF, TOGGLE, none (each option is described with English, Romanian and Russian verb examples)
- `scenario`
- `mower_action`

The decision is built from the answers the chosen intent needs. Its confidence is the lowest confidence among those answers.

**How the Claude baseline is asked.** It sends the production request (`VoiceTools.SYSTEM_PROMPT`, `TOOLS`, the cached catalog) and times only the first call, the tool selection that Jev would replace. It executes nothing.

### Decision criteria (agreed before running)

Jev is viable as a fast path, falling back to Claude below a confidence threshold, if all of these hold:

- p90 latency ≤ 500 ms from the local network
- fully correct commands within 2 percentage points of the Claude baseline, on EN and on RO
- ≥ 95% of cases give the same decision on every repeat
- ≤ 1% of control decisions are wrong with confidence ≥ 0.9 (these would switch the wrong device)

RU results are reported but don't decide the outcome, because the clients don't offer ru-RU speech-to-text yet.

### Results

Setup:

- Run on 2026-09-30 against one installation's catalog of about 100 peripherals and zones.
- Jev model: `jev-1.13.0`, about 5.5k input tokens per call.
- Claude model: Haiku 4.5, about 8k input tokens per call, with the catalog served from the prompt cache.
- Cases: 33 intents × EN/RO/RU = 99 cases, each repeated 5 times, so 495 calls per model.
- Calls ran sequentially from a Windows development machine on the home network. There were no errors.

The intents covered:

- devices by name, and whole rooms
- Romanian without diacritics
- simulated speech-to-text errors
- scenarios, state queries and the mower
- one ambiguous request and one off-topic request

**Speed** (ms per call, client wall time):

| | p50 | p90 | p99 | max |
|---|---|---|---|---|
| Jev | 379 | 465 | 630 | 801 |
| Claude Haiku 4.5, first turn only | 1,284 | 1,708 | 2,958 | 6,089 |

Jev is about 3.4× faster. In production, Claude also needs a second call to phrase the reply, so the real gap per command is larger.

**Correctness and consistency.** Jev was run twice: first with English-only descriptions of the on/off verbs, then with Romanian and Russian verb examples added to the `action` options (the version now in the script).

| Metric | Jev, English-only verbs | Jev, with RO/RU verbs | Claude |
|---|---|---|---|
| Correct, EN / RO / RU¹ | 87.9 / 87.9 / 84.8% | 87.9 / **93.3** / 87.9% | 91.5 / 91.5 / 82.4% |
| Correct, all languages¹ | 86.9% | **89.7%** | 88.5% |
| Action (on/off/toggle) correct | 94.9% | **98.7%** | 86.7% |
| Same decision on all 5 repeats | 96.0% | **98.0%** | 79.8% |
| Wrong command executed, no confidence gate | — | 10.3% | 6.7% |
| Cost per 1000 commands | $0.23 | $0.23 | $1.55 (first call only) |

¹ "Ask the user" and "no action" count as the same outcome. The Claude baseline cannot tell these apart reliably: a text reply ending in "?" is scored as a clarification, and any other text reply as "no action".

**Jev at different confidence gates** (with RO/RU verbs; decisions below the gate go to Claude):

| Gate | Share Jev handles | Accuracy | Wrong decisions (of 495) |
|---|---|---|---|
| ≥ 0.95 | 34% | 100% | 0 |
| ≥ 0.9 | 47% | 98.7% | 3 |
| ≥ 0.85 | 59% | 97.6% | 7 |
| ≥ 0.8 | 67% | 96.7% | 11 |
| ≥ 0.7 | 79% | 94.9% | 20 |

### Against the criteria (Jev with RO/RU verbs)

| Criterion | Result | Verdict |
|---|---|---|
| p90 ≤ 500 ms | 465 ms | pass (519 ms in the first run; network variance) |
| Within 2 pp of Claude, EN and RO | EN −3.6 pp, RO +1.8 pp | **EN fails**, RO passes |
| Consistency ≥ 95% | 98.0% | pass |
| Control decisions wrong at confidence ≥ 0.9 ≤ 1% | 0.7% (3/405) | pass |

### What the misses show

- **Verb examples in the user's languages matter.** With English-only descriptions, Romanian "stinge" ("turn off") and Russian "перекрой" ("shut off") often came back as ON or TOGGLE, always with low confidence. Adding RO/RU examples brought action accuracy to 100% in both languages.
- **Both models make the same risky mistake: whole zone vs. a dedicated circuit.** "Turn off all the outside lights" resolved to the whole outdoor zone, which also holds non-light devices, instead of the circuit that exists for exactly that. Every model, every language, every repeat got this wrong. Jev's confidence was 0.77–0.93. This is a catalog-naming issue, and aliases or a fast-path rule are the fix: never switch a whole zone OFF below a very high confidence, or leave that decision to the LLM.
- **Jev never answers `clarify`.** An ambiguous room ("the bedroom", when there are several) became one zone, with confidence of about 0.4. Ambiguity shows up as low confidence instead, and a gate routes it to Claude, which asks a proper question.
- **Vocabulary the catalog doesn't carry gives `none` from Jev** (for example English/Russian "basement" against a Romanian name). This is a safe miss. Claude either asked "did you mean …?" or occasionally picked a similarly spelled wrong device. Aliases fix it for both.
- **Claude is far less consistent.** It changed its decision across repeats in 20 of 99 cases: between asking, acting and giving up, and between a device and its zone. Twice it chose a wrong device whose name is spelled similarly.
- **Jev is nearly, but not fully, deterministic.** Confidence varies slightly between repeats (for example 0.87 vs 0.92), and in 2 of 99 cases the decision changed.

### With voice aliases

In the third run, both models used a catalog with voice aliases on most entities:

- **Coverage:** 43 of the 75 peripherals and 25 of the 26 zones got natural EN/RO/RU names, 179 aliases in total. They were written like the ones an operator would type into `feature.voice.alias`, for every main device and room, not only the ones that had failed.
- **Where they were added:** through an eval-only overlay (`tools/voice-eval/data/aliases.json`, merged at export), not in the database.
- **Prompt size:** the catalog grew from about 5.5k to 6.8k Jev input tokens.
- **Caveat:** whoever wrote the aliases knew the test phrases, so the gain is an upper bound. Confirm it on commands that were not in the test set.

| Metric | Jev, no aliases | Jev, aliases | Claude, no aliases | Claude, aliases |
|---|---|---|---|---|
| Correct, EN / RO / RU¹ | 87.9 / 93.3 / 87.9% | **93.9 / 97.0 / 97.0%** | 91.5 / 91.5 / 82.4% | 97.0 / 90.9 / 95.2% |
| Correct, all languages¹ | 89.7% | **96.0%** | 88.5% | 94.3% |
| Target correct | 92.6% | **100%** | 81.2% | 90.2% |
| Same decision on all 5 repeats | 98.0% | **99.0%** | 79.8% | 86.9% |
| Wrong command executed, no gate | 10.3% | 4.0% | 6.7% | 4.8% |
| Latency p50 / p90 | 379 / 465 ms | 607 / 1,080 ms² | 1,284 / 1,708 ms | 1,413 / 2,034 ms |
| Cost per 1000 commands | $0.23 | $0.29 | $1.55 | $1.51 |

² **Latency varies with Jev's service load.** An interleaved A/B one hour later measured the same 45 cases twice with and twice without aliases:

- without aliases: p50 of 618 and 738 ms
- with aliases: p50 of 744 and 1,032 ms

The catalog without aliases had given a p50 of 379 ms two hours earlier. So most of the slowdown came from the service, and the extra alias tokens add perhaps 100–300 ms. Claude slowed by only about 10% over the same period. Jev's docs say rate limits are "adjusting dynamically" due to demand.

**Jev at different confidence gates, with aliases:**

| Gate | Share Jev handles | Accuracy | Wrong decisions (of 495) |
|---|---|---|---|
| ≥ 0.95 | 56% | 100% | 0 |
| ≥ 0.9 | 73% | 100% | 0 |
| ≥ 0.8 | 82% | 100% | 0 |
| ≥ 0.7 | 88% | 100% | 0 |

All of Jev's remaining misses had confidence ≤ 0.65:

- the ambiguous-room request, where Jev still picks a zone instead of asking
- one misheard "turn of[f]" that came back as TOGGLE

With aliases, Jev got the "all outside lights" circuit right on every repeat and in every language, the error that had previously affected both models. **Claude still switched the whole outdoor zone OFF in every run, even with the alias present.**

### Local models: Laya and BERT / SetFit

Same cases and the same catalog with aliases, run on CPU on the development machine (Intel Core Ultra 7 155U, no GPU). An i5 server will be somewhat slower.

- **Laya** (`laya-multilingual`, 322M parameters, Apache-2.0) is an open-source "System One" model with a Jev-compatible question format. It asked the same questions as Jev.
  - It warns that accuracy drops past about 20 options per question, so the 102-option target question went through `laya.predict_shortlist`, which embeds and ranks the options, keeps the top 20, then makes one Laya call.
  - A flat 102-option question needs `max_len=4096` and took 13–20 s per call on CPU.
  - Laya's automatic routing would send Latin-script Romanian to its English-only checkpoint, so the multilingual checkpoint was forced.
  - It ran 1 repeat; see the Consistency column.
- **BERT embeddings + classifier heads** (`eval_setfit.py`):
  - The training data is generated from catalog names and aliases with EN/RO/RU verb templates, about 7k sentences. It deliberately avoids the test phrasings, and the test cases are never used for training.
  - Logistic-regression heads for intent, target, action, scenario and mower action sit on a multilingual sentence encoder.
  - **SetFit** first fine-tunes the encoder contrastively on the target labels. On CPU that took 13 minutes for MiniLM and 15 minutes for e5-small.
  - The **frozen** variant skips fine-tuning; fitting its heads took about 2 minutes.

| Model | p50 / p90 latency | Fully correct EN / RO / RU | Fully correct, all | Target | Action | Consistent | Share at confidence ≥ 0.9 (accuracy) |
|---|---|---|---|---|---|---|---|
| Laya multilingual, shortlist k=20 | 2,991 / 3,361 ms | 3.0 / 0.0 / 6.1% | 3.0% | 0% | 60.3% | 100% (1 repeat) | 1% |
| MiniLM-L12 multilingual, **frozen** + heads | **20 / 27 ms** | 84.8 / 69.7 / 81.8% | **78.8%** | 92.9% | 91.0% | 100% | 33% (100%) |
| MiniLM-L12 multilingual, SetFit fine-tuned + heads | 20 / 28 ms | 78.8 / 60.6 / 63.6% | 67.7% | 91.7% | 82.1% | 100% | 15% (100%) |
| multilingual-e5-small, SetFit fine-tuned + heads | 23 / 30 ms | 51.5 / 39.4 / 57.6% | 49.5% | 90.5% | 56.4% | 100% | 4% (100%) |
| *For reference: Jev, aliases* | *607 / 1,080 ms* | *93.9 / 97.0 / 97.0%* | *96.0%* | *100%* | *98.7%* | *99%* | *73% (100%)* |

Findings:

- **Laya is not usable zero-shot for this task.**
  - Target accuracy was 0%. The shortlist often dropped the right option, and Laya picked a wrong one even when the right option was kept.
  - It took about 3 s per call on CPU.
  - This matches its own documentation: the base checkpoints score close to random zero-shot, and the published accuracy needs fine-tuning on the task.
- **Embeddings pick the target well, but confuse opposite verbs.**
  - With frozen embeddings, target accuracy (93%) is on par with Jev without aliases.
  - Most misses are the on/off direction ("stinge" read as ON, "aprinde" as OFF), zone-versus-device choices, and the mower command.
  - All models were deterministic and never wrong at confidence ≥ 0.9, but only a third of the commands reached that confidence.
- **SetFit fine-tuning made it worse.** Contrastive training on target labels teaches the encoder that "turn on X" and "turn off X" are the same class, so the action head loses its signal: action accuracy fell from 91% to 82% for MiniLM, and to 56% for e5.
  - A fine-tune would need joint (target × action) labels, or separate encoders.
  - The cheaper fix is a deterministic on/off verb lexicon, since the verb set is small and closed.
- **Speed and cost:**
  - Local embedding inference takes about 20 ms on CPU, 20–50× faster than Jev and about 60× faster than Claude's first call.
  - It works offline, costs nothing per call, and needs about 0.5 GB of RAM.
  - The heads must be re-fitted (about 2 minutes) whenever the catalog or its aliases change.

### Improving BERT

All variants use frozen encoders; SetFit fine-tuning was dropped (see above). Changes were tested one at a time on the same cases (5 repeats, deterministic):

| Change | What it does |
|---|---|
| **Own-row heads** | Each head is trained only on its own examples: target on control/query, action on control, scenario on scenario, mower on mower. Previously every head also learned a dominant `none` class, which drowned the rare intents. |
| **Accent-stripped copies** | Every training sentence is added again without diacritics, since speech-to-text often drops Romanian ones. |
| **Verb lexicon** | A deterministic EN/RO/RU list of on/off/toggle verb stems, matched on lower-cased, accent-stripped text. When a stem matches, it sets the action (for example "stinge / opreste / inchide", "выключ / перекр", "turn off / shut / close"). |
| **Balanced heads** | Class-weighted logistic regression, so the mower and clarify intents are not outvoted by thousands of control examples. |
| **Encoder** | paraphrase-multilingual-MiniLM-L12 (118M), paraphrase-multilingual-mpnet-base (278M), multilingual-e5-small (118M), multilingual-e5-base (278M). |

| Encoder + changes | p50 / p90 | Fully correct EN / RO / RU | All | Target | Action | Share ≥ 0.9 (accuracy) | Wrong at ≥ 0.9 |
|---|---|---|---|---|---|---|---|
| MiniLM, first version | 20 / 27 ms | 84.8 / 69.7 / 81.8% | 78.8% | 92.9% | 91.0% | 33% (100%) | 0 |
| MiniLM + own-row heads | 16 / 22 ms | 84.8 / 78.8 / 84.8% | 82.8% | 92.9% | 91.0% | 33% (100%) | 0 |
| … + accent copies | 20 / 26 ms | 81.8 / 69.7 / 81.8% | 77.8% | 92.9% | 87.2% | 43% (95.3%) | 10 |
| … + accent copies + lexicon | 24 / 37 ms | 84.8 / 81.8 / 84.8% | 83.8% | 92.9% | 94.9% | 48% (97.9%) | 5 |
| **MiniLM + own-row heads + lexicon** | **22 / 28 ms** | 87.9 / 84.8 / 84.8% | 85.9% | 92.9% | 96.2% | **40% (100%)** | 0 |
| … + balanced | 20 / 28 ms | 87.9 / 81.8 / 87.9% | 85.9% | 90.5% | 94.9% | 32% (100%) | 0 |
| mpnet-base + accent copies + lexicon | 39 / 50 ms | 84.8 / 87.9 / 84.8% | 85.9% | 92.9% | 94.9% | 49% (95.9%) | 10 |
| e5-small + lexicon | 23 / 31 ms | 84.8 / 81.8 / 81.8% | 82.8% | 95.2% | 96.2% | 13% (100%) | 0 |
| e5-small + accent copies + lexicon | 22 / 33 ms | 84.8 / 84.8 / 84.8% | 84.8% | 94.0% | 96.2% | 22% (100%) | 0 |
| e5-small + accent copies + lexicon + balanced | 26 / 33 ms | 84.8 / 84.8 / 90.9% | 86.9% | 92.9% | 96.2% | 13% (100%) | 0 |
| e5-base + lexicon | 41 / 51 ms | 84.8 / 84.8 / 81.8% | 83.8% | 96.4% | 97.4% | 8% (87.5%) | 5 |
| e5-base + accent copies + lexicon | 40 / 57 ms | 84.8 / 84.8 / 87.9% | 85.9% | 97.6% | 97.4% | 15% (100%) | 0 |
| **e5-base + accent copies + lexicon + balanced** | **41 / 53 ms** | **93.9 / 90.9 / 93.9%** | **92.9%** | **97.6%** | 96.2% | 9% (100%) | 0 |
| *Jev, aliases (reference)* | *607 / 1,080 ms* | *93.9 / 97.0 / 97.0%* | *96.0%* | *100%* | *98.7%* | *73% (100%)* | *0* |

What worked:

- **Own-row heads (+4 pp)** and the **verb lexicon (+3 pp, and action accuracy up to 96–97%)** are the two clear wins. Both are free at inference time.
- **Accent-stripped copies help e5 but hurt MiniLM.** For MiniLM, they made the confidence worse calibrated: ten wrong answers at ≥ 0.9.
- **e5-base with every change reaches 92.9%, within 3 pp of Jev with aliases, at about 40 ms.** Balanced heads spread its probabilities, so few answers pass 0.9. Its 0.5–0.9 band is still 96.9% accurate, so e5-base should be gated around 0.6 rather than 0.9 (see the cascade below).
- **The remaining BERT misses are low-confidence and structural:**
  - ambiguous rooms, which BERT cannot recognise as "ask the user"
  - off-topic questions
  - the zone-versus-device choice for plural "lights"
  - mower commands phrased unlike the training templates

  A gate sends all of these on to Jev or Claude.
- **Caveats:**
  - The verb lexicon and the aliases were written by someone who had seen the test set.
  - Latency was measured on a laptop CPU. An i5 will be slower, but should still be well under 100 ms. ONNX/int8 export through sentence-transformers is the usual next step.

### Cascade: BERT → Jev → Claude

`Simulate-Cascade.ps1` replays the recorded runs, all on the same 495 calls with the alias catalog:

- **Routing:** a stage answers when its decision is actionable (control, scenario, query or mower) and its confidence is at or above that stage's gate; otherwise the call moves on. Claude always answers, so clarifications and off-topic requests end up there.
- **Latency:** the sum of the stages a call passed through. For Claude only the first call is counted, and the Jev numbers come from its slower evening run.
- **Wrong executed:** a command carried out on the wrong device or with the wrong action. It includes Claude's own mistakes, such as the whole outdoor zone for "all outside lights".

| Setup | Correct | Wrong executed | Handled by BERT / Jev / Claude | p50 | p90 | Mean |
|---|---|---|---|---|---|---|
| Claude only (today, first call) | 94.3% | 5.1% | 0 / 0 / 100% | 1,413 ms | 2,034 ms | 1,533 ms |
| Jev ≥ 0.7 → Claude | 98.6% | 1.4% | 0 / 85 / 15% | 684 ms | 2,055 ms | 987 ms |
| MiniLM ≥ 0.9 → Jev ≥ 0.7 → Claude | 98.6% | 1.4% | 40 / 45 / 15% | 507 ms | 2,048 ms | 718 ms |
| e5-base ≥ 0.8 → Jev ≥ 0.7 → Claude | 98.6% | 1.4% | 23 / 64 / 13% | 627 ms | 1,990 ms | 838 ms |
| **e5-base ≥ 0.6 → Jev ≥ 0.7 → Claude** | **97.8%** | **2.2%** | **62 / 29 / 10%** | **50 ms** | 1,841 ms | **497 ms** |
| e5-base ≥ 0.5 → Jev ≥ 0.7 → Claude | 97.0% | 3.0% | 72 / 20 / 9% | 46 ms | 1,163 ms | 405 ms |
| MiniLM ≥ 0.8 → Jev ≥ 0.7 → Claude | 96.6% | 3.4% | 60 / 28 / 13% | 26 ms | 1,925 ms | 549 ms |
| e5-base ≥ 0.6 → Claude (no Jev) | 95.6% | 3.8% | 62 / 0 / 38% | 50 ms | 1,664 ms | 621 ms |

The full gate grid is in `results/cascade-*-summary.md`.

- **The cascade is both faster and more accurate than Claude alone.** Every configuration above beats Claude-only on correctness and on wrong executions, because BERT and Jev resolve the device more consistently than Claude, and Claude is left with the conversational cases it handles best.
- **Three sensible operating points:**
  - **Most accurate:** MiniLM ≥ 0.9 → Jev ≥ 0.7. It matches the best accuracy seen (98.6%), with 40% of commands answered locally.
  - **Balanced:** e5-base ≥ 0.6 → Jev ≥ 0.7. It gives up 0.8 pp of accuracy, but 62% of commands are answered locally in about 50 ms, and the mean latency is a third of Claude's.
  - **Without Jev:** e5-base ≥ 0.6 → Claude. It is still better than today on every metric.
- **Offline:** when the cloud is unreachable, the local BERT stage alone covers about 60–70% of commands. The rest can get a templated "I can't do that offline" reply.

### Encoder decision (production sidecar, int8 ONNX)

The sidecar (`bridges/voice-nlu`) runs the encoders as int8 ONNX, with each encoder's best measured training variant. `tools/voice-eval/eval_sidecar.py` measured it on the same cases, in-process on the development CPU:

| Encoder | Fully correct (Python → int8) | p50 / p90 | Heads training | 0.5–0.9 band accuracy |
|---|---|---|---|---|
| minilm | 85.9% → 85.9% | 8 / 11 ms | 111 s | 83.7% |
| **e5-small** | 86.9% → 85.9% | **9 / 14 ms** | 165 s | **98.5%** |
| e5-base | 92.9% → 91.9% | 30 / 42 ms | 331 s | 96.7% |

Quantisation costs at most 1 pp of accuracy, and the ONNX runtime is 2–3× faster than PyTorch. Over HTTP, the round trip for e5-small was 19 ms p50.

The cascade with the int8 runs, using the same Jev and Claude runs as above:

| First stage | Gate | Correct | Wrong executed | Handled by local / Jev / Claude | p50 | Mean |
|---|---|---|---|---|---|---|
| minilm → Jev ≥ 0.7 → Claude | 0.9 | 98.6% | 1.4% | 38 / 47 / 14% | 498 ms | 713 ms |
| **e5-small → Jev ≥ 0.7 → Claude** | **0.6** | 97.6% | 2.4% | **63 / 27 / 11%** | **13 ms** | **471 ms** |
| e5-small → Jev ≥ 0.7 → Claude | 0.5 | 97.8% | 2.2% | 70 / 21 / 10% | 11 ms | 405 ms |
| e5-base → Jev ≥ 0.7 → Claude | 0.6 | 97.8% | 2.2% | 55 / 36 / 10% | 43 ms | 541 ms |
| **e5-small → Claude (phase 2, no Jev)** | **0.6** | 95.6% | 3.8% | 63 / 0 / 37% | 13 ms | 576 ms |

**Decision: `e5-small` is the default encoder, with gate 0.6.**

- It is the size of MiniLM (113 MB), runs at about 10 ms, and answers about 63% of commands locally.
- Its confidence is well calibrated: 98.5% accurate in the 0.5–0.9 band.
- Even without Jev, it beats Claude alone on every metric.
- 0.6 is used rather than 0.5 as a margin for unseen phrasings. Production shadow logs should confirm the gate.
- `e5-base` remains the option if accuracy matters more than RAM; `minilm` at 0.9 if fewer, safer local decisions are wanted.

### Held-out check (new phrasings)

All numbers above come from `cases.json`, and that set is not independent: the templates, the verb list, the aliases and the encoder choice were all made while looking at it. To measure fairly, a separate held-out set was written: 35 intents × EN/RO/RU = 105 phrasings.

- **What it covers:** mostly devices and rooms `cases.json` doesn't, phrased unlike the templates. That includes verbs outside the verb list ("kill the light", "pune banda led"), typos, a radio request that starts with a control verb, a zone-wide OFF, and two ambiguous requests.
- **Where it lives:** it names the installation's devices, so it is kept with the deployment (mounted into the sidecar via `NLU_HELDOUT`), not in this repository.
- **What it is for:** it is also the set that gates the sidecar's daily retrain.

| Encoder (int8 sidecar) | `cases.json` | Held-out | Accuracy, confidence 0.5–0.9 | Wrong at ≥ 0.9 |
|---|---|---|---|---|
| **e5-small** | 85.9% | **76.2%** | **97.9%** | 0 |
| minilm | 85.9% | 67.6% | 79.6% | 0 |
| e5-base | 91.9% | 70.5% | 91.8% | 0 |

- **Accuracy drops on unseen phrasings,** as expected. **e5-small is the best of the three** there; e5-base's lead on `cases.json` was fitted to that set. This confirms the default.
- **The gate holds on unseen data.** With e5-small, a gate of 0.6 acts on 32% of the held-out commands, and a gate of 0.5 on 42%, **both with no wrong decisions**. Every miss had confidence ≤ 0.52 and would have fallen through to Jev or Claude. Gate 0.5 is a candidate once production shadow logs agree.
- **Where it is weakest:**
  - devices of a different kind sharing a room's name (a room's heater picked when its temperature sensor was meant)
  - pause and resume for the mower
  - sprinkler program names
  - ambiguity, which the model cannot express except as low confidence

  These are the gaps the learning loop and better aliases should close; their effect will show on this held-out set.

### Conclusion

**Recommended architecture: BERT (local) → Jev → Claude.**

- **BERT first:**
  - A frozen multilingual encoder runs on the server's CPU: e5-small by default (see "Encoder decision"), with e5-base and MiniLM as alternatives.
  - Its heads are re-fitted from the catalog and aliases whenever they change, which takes 2–5 minutes.
  - An EN/RO/RU verb lexicon sets the action.
  - It answers most commands in tens of milliseconds, offline and at no cost per call.
- **Jev second, for what BERT isn't sure about:**
  - It resolves most of the remainder in about 0.4–1 s.
  - Its confidence is a reliable gate once aliases exist.
  - Its latency varied between 380 and 1,000 ms p50 over one evening, so measure it over several days, from the server.
- **Claude last,** for ambiguity, clarifying questions, off-topic requests and anything complex or conversational: the existing tool loop, unchanged.
- **Aliases matter more than the model choice.** They lifted Jev from 90% to 96%, improved Claude and BERT, and fixed the one risky mistake. Configure them for the main devices and rooms.
- **Also needed:**
  - templated spoken replies for BERT and Jev decisions, since neither produces text
  - a guard on zone-wide OFF commands
  - per-stage timing in `VoiceCommandService`
- **Jev is cloud-only and in early access.** The cascade degrades gracefully without it: BERT → Claude is still better than today.
