"""Deterministic on/off/toggle verbs (EN/RO/RU).

Matched on lower-cased, accent-stripped text; the lists are checked in order and the first
one with a hit wins. A hit overrides the action head, which confuses opposite verbs.
"""
import re
import unicodedata

VERB_LEXICON = [
    ("TOGGLE", [r"\btoggle\b", r"\bflip\b", r"\bcomut", r"\bschimba starea\b", r"переключ"]),
    ("OFF", [r"\bturn(ed)? (it )?of+\b", r"\bswitch(ed)? of+\b", r"\bshut\b", r"\bclose\b", r"\bstop\b",
             r"\b(lights?|it) off\b", r"\boff\b", r"\bstinge", r"\bopreste", r"\binchide", r"\btaie\b",
             r"выключ", r"погас", r"закр", r"перекр", r"останов", r"отключ"]),
    ("ON", [r"\bturn(ed)? (it )?on\b", r"\bswitch(ed)? on\b", r"\blight up\b", r"\bopen\b", r"\bstart\b",
            r"\bon\b", r"\baprinde", r"\bporneste", r"\bdeschide", r"\bda(-i)? drumul\b",
            r"включ", r"зажг", r"откр", r"запуст"]),
]


def strip_accents(text):
    return "".join(c for c in unicodedata.normalize("NFD", text) if unicodedata.category(c) != "Mn")


_COMPILED = [(action, [re.compile(strip_accents(p)) for p in patterns]) for action, patterns in VERB_LEXICON]


def lexicon_action(text):
    """The action named by the verb in `text`, or None when no known verb is present."""
    t = strip_accents(text.lower())
    for action, patterns in _COMPILED:
        if any(p.search(t) for p in patterns):
            return action
    return None
