"""Local BERT / SetFit intent resolver, evaluated on data/cases.json.

Training data is generated from data/catalog.json (names + aliases) with EN/RO/RU verb
templates; the test cases are never used for training. A multilingual sentence encoder is
fine-tuned with SetFit on the target labels (entities), then logistic-regression heads for
intent, target, action, scenario and mower action are fitted on its embeddings.
--finetune-steps 0 skips fine-tuning: plain frozen BERT embeddings + heads.

Writes results/<tag>-<ts>.jsonl in the format Summarize-Results.ps1 scores.

    .venv/Scripts/python eval_setfit.py
    .venv/Scripts/python eval_setfit.py --base intfloat/multilingual-e5-small
    .venv/Scripts/python eval_setfit.py --finetune-steps 0
"""
import argparse
import json
import random
import sys
import time
from collections import defaultdict
from datetime import datetime
from pathlib import Path

import numpy as np
from sklearn.linear_model import LogisticRegression

ROOT = Path(__file__).parent
DATA = ROOT / "data"
RESULTS = ROOT / "results"

# Templates, verb lexicon and row generation are shared with the production sidecar.
sys.path.insert(0, str(ROOT.parent.parent / "bridges" / "voice-nlu"))
from voice_nlu.data import build_training  # noqa: E402
from voice_nlu.lexicon import lexicon_action  # noqa: E402


def top(probs, classes, n=3):
    idx = np.argsort(probs)[::-1][:n]
    return " ".join(f"{classes[i]}={probs[i]:.2f}" for i in idx)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
    ap.add_argument("--finetune-steps", type=int, default=400)
    ap.add_argument("--per-class", type=int, default=16, help="examples per target for SetFit fine-tuning")
    ap.add_argument("--repeat", type=int, default=5)
    ap.add_argument("--langs", default="en,ro,ru")
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--heads", choices=["v1", "v2"], default="v2",
                    help="v1: every head trained on all rows ('none' for unrelated); v2: each head only on its own rows")
    ap.add_argument("--no-augment", action="store_true", help="skip accent-stripped training copies")
    ap.add_argument("--no-lexicon", action="store_true", help="action from the classifier head only")
    ap.add_argument("--balanced", action="store_true",
                    help="class-balanced heads, so rare intents (mower, clarify) are not drowned by control")
    args = ap.parse_args()

    import torch
    from setfit import SetFitModel, Trainer, TrainingArguments
    from datasets import Dataset

    random.seed(args.seed)
    np.random.seed(args.seed)
    torch.manual_seed(args.seed)

    catalog = json.loads((DATA / "catalog.json").read_text(encoding="utf-8"))
    cases = json.loads((DATA / "cases.json").read_text(encoding="utf-8"))
    prefix = "query: " if "e5" in args.base.lower() else ""

    rows = build_training(catalog, augment=not args.no_augment)
    print(f"training rows: {len(rows)}")

    model = SetFitModel.from_pretrained(args.base)
    t0 = time.perf_counter()
    if args.finetune_steps > 0:
        by_target = defaultdict(list)
        for r in rows:
            by_target[r[2]].append(r[0])
        texts, labels = [], []
        for target, items in by_target.items():
            for t in random.sample(items, min(args.per_class, len(items))):
                texts.append(prefix + t)
                labels.append(target)
        trainer = Trainer(
            model=model,
            args=TrainingArguments(batch_size=32, num_epochs=1, max_steps=args.finetune_steps,
                                   body_learning_rate=2e-5, seed=args.seed),
            train_dataset=Dataset.from_dict({"text": texts, "label": labels}),
        )
        trainer.train()
    body = model.model_body
    train_seconds = time.perf_counter() - t0

    t0 = time.perf_counter()
    emb = body.encode([prefix + r[0] for r in rows], batch_size=64, normalize_embeddings=True,
                      show_progress_bar=False)
    heads = {}
    own_rows = {"intent": None, "target": ("control", "query"), "action": ("control",),
                "scenario": ("scenario",), "mower": ("mower",)}
    for i, name in enumerate(["intent", "target", "action", "scenario", "mower"], start=1):
        keep = np.array([args.heads == "v1" or own_rows[name] is None or r[1] in own_rows[name] for r in rows])
        y = np.array([r[i] for r in rows])[keep]
        heads[name] = LogisticRegression(max_iter=3000, C=10.0,
                                         class_weight="balanced" if args.balanced else None).fit(emb[keep], y)
    head_seconds = time.perf_counter() - t0
    print(f"fine-tune: {train_seconds:.0f}s, heads: {head_seconds:.0f}s")

    def decide(text):
        e = body.encode([prefix + text], normalize_embeddings=True, show_progress_bar=False)
        out = {n: (h.predict_proba(e)[0], h.classes_) for n, h in heads.items()}

        def pick(n):
            probs, classes = out[n]
            i = int(np.argmax(probs))
            return classes[i], float(probs[i])

        intent, ci = pick("intent")
        pred = {"intent": intent, "target": None, "action": None, "scenario": None, "mowerAction": None}
        used = {"intent": ci}
        if intent in ("control", "query"):
            pred["target"], used["target"] = pick("target")
        lex = None
        if intent == "control":
            lex = None if args.no_lexicon else lexicon_action(text)
            pred["action"], used["action"] = (lex, 1.0) if lex else pick("action")
        if intent == "scenario":
            pred["scenario"], used["scenario"] = pick("scenario")
        if intent == "mower":
            pred["mowerAction"], used["mowerAction"] = pick("mower")
        pred = {k: (None if v == "none" else v) for k, v in pred.items()}
        tops = " / ".join(f"{n}: {top(*out[n])}" for n in ["intent"] + [
            {"target": "target", "action": "action", "scenario": "scenario", "mowerAction": "mower"}[k]
            for k in used if k != "intent"])
        if lex:
            tops += f" (action from lexicon: {lex})"
        return pred, min(used.values()), tops

    langs = args.langs.split(",")
    work = [(f"{c['id']}.{l}", l, c["text"][l]) for c in cases for l in langs if c["text"].get(l)]
    decide(work[0][2])  # warm-up

    RESULTS.mkdir(exist_ok=True)
    tag = "setfit" if args.finetune_steps > 0 else "embed"
    tag += "-" + args.base.split("/")[-1] + f"-heads{args.heads}"
    tag += ("" if args.no_augment else "-aug") + ("" if args.no_lexicon else "-lex") + ("-bal" if args.balanced else "")
    out_file = RESULTS / f"{tag}-{datetime.now():%Y%m%d-%H%M%S}.jsonl"
    with out_file.open("w", encoding="utf-8") as f:
        for rep in range(1, args.repeat + 1):
            for case_id, lang, text in work:
                t = time.perf_counter()
                pred, conf, tops = decide(text)
                ms = (time.perf_counter() - t) * 1000
                f.write(json.dumps({"CaseId": case_id, "Lang": lang, "Repeat": rep, "Ok": True,
                                    "Ms": round(ms, 1), "Pred": pred, "Confidence": round(conf, 4),
                                    "Top": tops}, ensure_ascii=False) + "\n")
    print(f"train: {train_seconds + head_seconds:.0f}s total; results: {out_file}")


if __name__ == "__main__":
    main()
