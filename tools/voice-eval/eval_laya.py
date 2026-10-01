"""Laya (open-source, local "System One" model) evaluated on data/cases.json.

Asks the same typed questions as Invoke-JevEval.ps1 (intent / target / action / scenario /
mower_action) against a local Laya checkpoint on CPU.

  --mode flat       every target option in one choice question (Laya warns accuracy drops
                    past ~20 options, so the head token budget is raised)
  --mode shortlist  laya.predict_shortlist: embed-rank each large choice down to --k options,
                    then one Laya call

The multilingual checkpoint is forced by default; Laya's automatic routing would send
Latin-script Romanian to the English-only checkpoint (--model auto tries that).

Writes results/laya-<mode>-<ts>.jsonl in the format Summarize-Results.ps1 scores.

    .venv/Scripts/python eval_laya.py --mode shortlist
"""
import argparse
import json
import time
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).parent
DATA = ROOT / "data"
RESULTS = ROOT / "results"


def join_some(names, limit):
    names = [n for n in names if n]
    if len(names) <= limit:
        return ", ".join(names)
    return ", ".join(names[:limit]) + f", ... ({len(names)} total)"


# Mirrors the questions built in Invoke-JevEval.ps1.
def build_questions(catalog):
    targets = {}
    for p in catalog["peripherals"]:
        d = f"DEVICE: {p['name']} ({p.get('category')})"
        if p.get("zones"):
            d += " in " + ", ".join(p["zones"])
        if p.get("aliases"):
            d += "; also called " + ", ".join(p["aliases"])
        targets[f"P{p['id']}"] = d
    for z in catalog["zones"]:
        d = f"AREA (everything in it): {z['name']}; contains {join_some(z.get('peripherals') or [], 12)}"
        if z.get("aliases"):
            d += "; also called " + ", ".join(z["aliases"])
        targets[f"Z{z['id']}"] = d
    targets["none"] = "Nothing in the list matches"

    questions = {
        "intent": {"type": "choice",
                   "instructions": "The text is a spoken request to a home-automation assistant (English, Romanian or Russian; it may contain speech-recognition errors). What kind of request is it?",
                   "criteria": {
                       "control": "Turn on/off, toggle, open/close or start/stop a device or a whole area (lights, heating, water valves, gates, sprinklers, switches, ventilation)",
                       "scenario": "Run a predefined automation/scenario, named or described",
                       "query": "Ask about a current state or value (temperature, is something on/open?) without changing anything",
                       "mower": "Command the robotic lawn mower: start, stop, pause or resume mowing, or send it to its dock",
                       "clarify": "A home command that is ambiguous: it could equally mean several different devices or rooms, so the user must say which",
                       "other": "Not a home-automation request (chit-chat, general knowledge)"}},
        "target": {"type": "choice",
                   "instructions": "Which single device or area does the user want to control or ask about? Catalog names are Romanian; the user may speak English, Romanian or Russian. Choose an AREA when the user means everything in a place, a DEVICE when they mean one specific device or one kind of device in a room.",
                   "criteria": targets},
        "action": {"type": "choice", "instructions": "Which on/off action does the user request?",
                   "criteria": {
                       "ON": "Turn on, switch on, light up, open (a valve, water, gate), start. Romanian: aprinde, pornește, deschide, dă drumul. Russian: включи, зажги, открой, запусти",
                       "OFF": "Turn off, switch off, close or shut off (a valve, water), stop. Romanian: stinge, oprește, închide, taie. Russian: выключи, погаси, закрой, перекрой, останови",
                       "TOGGLE": "Toggle or flip to the opposite state. Romanian: comută, schimbă. Russian: переключи",
                       "none": "No on/off action is requested"}},
        "mower_action": {"type": "choice", "instructions": "Which robotic lawn mower command is requested?",
                         "criteria": {"START": "Start mowing", "STOP": "Stop mowing", "PAUSE": "Pause mowing",
                                      "RESUME": "Resume mowing", "DOCK": "Return to the charging dock / go home",
                                      "none": "No mower command"}},
    }
    if catalog.get("scenarios"):
        sc = {f"S{s['jobId']}": f"{s['name']}: {s['description']}" for s in catalog["scenarios"]}
        sc["none"] = "No scenario is requested"
        questions["scenario"] = {"type": "choice", "instructions": "Which predefined automation scenario should be run?",
                                 "criteria": sc}
    return questions


def top(answer, n=3):
    probs = answer.get("probabilities") or {}
    return " ".join(f"{k}={v:.2f}" for k, v in sorted(probs.items(), key=lambda kv: -kv[1])[:n])


def decide(answers):
    intent = answers["intent"]["choice"]
    pred = {"intent": intent, "target": None, "action": None, "scenario": None, "mowerAction": None}
    used = ["intent"]
    if intent in ("control", "query"):
        pred["target"] = answers["target"]["choice"]; used.append("target")
    if intent == "control":
        pred["action"] = answers["action"]["choice"]; used.append("action")
    if intent == "scenario" and "scenario" in answers:
        pred["scenario"] = answers["scenario"]["choice"]; used.append("scenario")
    if intent == "mower":
        pred["mowerAction"] = answers["mower_action"]["choice"]; used.append("mower_action")
    pred = {k: (None if v == "none" else v) for k, v in pred.items()}
    conf = min(float(answers[q]["confidence"]) for q in used)
    tops = " / ".join(f"{q}: {top(answers[q])}" for q in used)
    return pred, conf, tops


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mode", choices=["flat", "shortlist"], default="shortlist")
    ap.add_argument("--model", default="multilingual", help="multilingual | english | auto")
    ap.add_argument("--k", type=int, default=20)
    ap.add_argument("--head-max-len", type=int, default=1024, help="flat mode head token budget")
    ap.add_argument("--repeat", type=int, default=5)
    ap.add_argument("--langs", default="en,ro,ru")
    args = ap.parse_args()

    import laya

    catalog = json.loads((DATA / "catalog.json").read_text(encoding="utf-8"))
    cases = json.loads((DATA / "cases.json").read_text(encoding="utf-8"))
    questions = build_questions(catalog)

    t0 = time.perf_counter()
    if args.model == "auto":
        # Router picks the checkpoint per request (English for Latin script, multilingual otherwise).
        agent = laya.Router()
        embed_agent = laya.load(*laya.DEFAULT_MODELS["multilingual"][:1], subfolder=laya.DEFAULT_MODELS["multilingual"][1])
    else:
        repo, sub = laya.DEFAULT_MODELS[args.model]
        agent = embed_agent = laya.load(repo, subfolder=sub)
    embed = laya.cached_embed_fn(laya.embed_fn_from_agent(embed_agent)) if args.mode == "shortlist" else None
    print(f"load: {time.perf_counter() - t0:.1f}s")

    def run(text):
        if args.mode == "shortlist":
            return laya.predict_shortlist(agent, text, questions, embed, k=args.k)
        return agent.predict(text, questions, head_max_len=args.head_max_len)

    langs = args.langs.split(",")
    work = [(f"{c['id']}.{l}", l, c["text"][l]) for c in cases for l in langs if c["text"].get(l)]
    run(work[0][2])  # warm-up (fills the option-embedding cache in shortlist mode)

    RESULTS.mkdir(exist_ok=True)
    out_file = RESULTS / f"laya-{args.mode}-{args.model}-{datetime.now():%Y%m%d-%H%M%S}.jsonl"
    with out_file.open("w", encoding="utf-8") as f:
        for rep in range(1, args.repeat + 1):
            for case_id, lang, text in work:
                rec = {"CaseId": case_id, "Lang": lang, "Repeat": rep}
                t = time.perf_counter()
                try:
                    result = run(text)
                    rec["Ms"] = round((time.perf_counter() - t) * 1000, 1)
                    pred, conf, tops = decide(result["answers"])
                    rec.update({"Ok": True, "Pred": pred, "Confidence": round(conf, 4), "Top": tops})
                except Exception as ex:  # keep going; the summary lists errors
                    rec.update({"Ok": False, "Ms": round((time.perf_counter() - t) * 1000, 1), "Error": str(ex)[:300]})
                f.write(json.dumps(rec, ensure_ascii=False) + "\n")
                print(f"[{rep}/{args.repeat}] {case_id:<32} {rec['Ms']:7.0f} ms  "
                      f"{rec.get('Pred') if rec['Ok'] else rec['Error'][:80]}", flush=True)
    print(f"results: {out_file}")


if __name__ == "__main__":
    main()
