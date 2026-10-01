"""Classifier heads over sentence embeddings and the decision built from them.

Each head is trained only on the rows it is meant for (target on control/query, action on
control, scenario on scenario, mower on mower); the intent head sees every row.
"""
import numpy as np
from sklearn.linear_model import LogisticRegression

from .lexicon import lexicon_action

OWN_ROWS = {"intent": None, "target": ("control", "query"), "action": ("control",),
            "scenario": ("scenario",), "mower": ("mower",)}
FIELDS = ["intent", "target", "action", "scenario", "mower"]


class ConstantHead:
    """Stands in for a head whose training rows have a single label (sklearn needs two)."""

    def __init__(self, label):
        self.classes_ = np.array([label])

    def predict_proba(self, x):
        return np.ones((len(x), 1))


def fit_heads(rows, embeddings, balanced=False):
    heads = {}
    for i, name in enumerate(FIELDS, start=1):
        keep = np.array([OWN_ROWS[name] is None or r[1] in OWN_ROWS[name] for r in rows])
        if not keep.any():
            continue
        y = np.array([r[i] for r in rows])[keep]
        if len(set(y)) == 1:
            heads[name] = ConstantHead(y[0])
            continue
        heads[name] = LogisticRegression(max_iter=3000, C=10.0,
                                         class_weight="balanced" if balanced else None).fit(embeddings[keep], y)
    return heads


def _top(probs, classes, n=3):
    idx = np.argsort(probs)[::-1][:n]
    return " ".join(f"{classes[i]}={probs[i]:.2f}" for i in idx)


def decide(text, embedding, heads, use_lexicon=True):
    """Decision for one utterance; confidence is the minimum over the heads the intent needs."""
    out = {n: (h.predict_proba(embedding[None, :])[0], h.classes_) for n, h in heads.items()}

    def pick(name):
        if name not in out:
            return "none", 0.0
        probs, classes = out[name]
        i = int(np.argmax(probs))
        return str(classes[i]), float(probs[i])

    intent, conf_intent = pick("intent")
    pred = {"intent": intent, "target": None, "action": None, "scenario": None, "mowerAction": None}
    used = {"intent": conf_intent}
    lex = None
    if intent in ("control", "query"):
        pred["target"], used["target"] = pick("target")
    if intent == "control":
        lex = lexicon_action(text) if use_lexicon else None
        pred["action"], used["action"] = (lex, 1.0) if lex else pick("action")
    if intent == "scenario":
        pred["scenario"], used["scenario"] = pick("scenario")
    if intent == "mower":
        pred["mowerAction"], used["mowerAction"] = pick("mower")
    pred = {k: (None if v == "none" else v) for k, v in pred.items()}

    head_of = {"intent": "intent", "target": "target", "action": "action", "scenario": "scenario",
               "mowerAction": "mower"}
    top = " / ".join(f"{k}: {_top(*out[head_of[k]])}" for k in used if head_of[k] in out)
    if lex:
        top += f" (action from lexicon: {lex})"
    return pred, min(used.values()), top
