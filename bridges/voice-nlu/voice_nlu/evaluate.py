"""Scoring against a held-out case set, used to gate retrained heads.

Cases use the tools/voice-eval format:
[{id, accept: [{intent, target, action, scenario, mowerAction}], text: {en, ro, ru}}]
A decision is correct when it matches every field of any accepted outcome; "clarify" and
"other" count as the same no-action outcome (as in the evaluation tooling).
"""
import json
from pathlib import Path

FIELDS = ("intent", "target", "action", "scenario", "mowerAction")


def load_cases(path):
    path = Path(path)
    if not path.exists():
        return []
    cases = json.loads(path.read_text(encoding="utf-8"))
    return [(text, case["accept"]) for case in cases for text in (case.get("text") or {}).values() if text]


def _norm(intent):
    return "noaction" if intent in ("clarify", "other") else intent


def correct(pred, accept):
    for a in accept:
        if all(a.get(f) is None or (_norm(a[f]) if f == "intent" else a[f]) ==
               (_norm(pred.get(f)) if f == "intent" else pred.get(f)) for f in FIELDS):
            return True
    return False


def accuracy(decide_text, cases):
    """Share of cases `decide_text(text) -> decision dict` gets fully right; None without cases."""
    if not cases:
        return None
    return sum(correct(decide_text(text), accept) for text, accept in cases) / len(cases)
