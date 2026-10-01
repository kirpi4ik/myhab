"""Training sentences generated from the voice catalog (names + aliases) with EN/RO/RU templates.

The catalog has the shape VoiceCommandService.buildCatalog() produces:
{peripherals: [{id, name, category, zones, aliases}], zones: [{id, name, peripherals, aliases}],
 scenarios: [{jobId, name, description}], mowers: [{deviceId, name}]}.

Rows are (text, intent, target, action, scenario, mower); 'none' marks a field that does not apply.
"""
from .lexicon import strip_accents

LIGHT_CATS = {"LIGHT"}
OPENABLE_CATS = {"VALVE", "SWITCH", "DOOR_LOCK", "SPRINKLER"}
SENSOR_CATS = {"TEMP", "Sensor", "LUMINOSITY", "MOTION"}
LANGS = ("en", "ro", "ru")

VERBS = {
    # (language, action) -> [(template, applies_to)] ; applies_to: all | light | open
    ("en", "ON"): [("turn on the {x}", "all"), ("switch on the {x}", "all"), ("{x} on please", "all"),
                   ("light up the {x}", "light"), ("open the {x}", "open")],
    ("en", "OFF"): [("turn off the {x}", "all"), ("switch off the {x}", "all"), ("{x} off", "all"),
                    ("close the {x}", "open"), ("shut off the {x}", "open")],
    ("en", "TOGGLE"): [("toggle the {x}", "all"), ("flip the {x}", "all")],
    ("ro", "ON"): [("pornește {x}", "all"), ("aprinde {x}", "light"), ("deschide {x}", "open"),
                   ("dă drumul la {x}", "open"), ("te rog pornește {x}", "all")],
    ("ro", "OFF"): [("oprește {x}", "all"), ("stinge {x}", "light"), ("închide {x}", "open"),
                    ("te rog oprește {x}", "all")],
    ("ro", "TOGGLE"): [("comută {x}", "all"), ("schimbă starea la {x}", "all")],
    ("ru", "ON"): [("включи {x}", "all"), ("зажги {x}", "light"), ("открой {x}", "open"),
                   ("пожалуйста включи {x}", "all")],
    ("ru", "OFF"): [("выключи {x}", "all"), ("погаси {x}", "light"), ("закрой {x}", "open"),
                    ("перекрой {x}", "open")],
    ("ru", "TOGGLE"): [("переключи {x}", "all")],
}
ZONE_TEMPLATES = {
    ("en", "ON"): ["turn on everything in the {x}", "all lights on in the {x}"],
    ("en", "OFF"): ["turn off everything in the {x}", "switch off all of the {x}"],
    ("ro", "ON"): ["pornește tot în {x}", "aprinde toate luminile din {x}"],
    ("ro", "OFF"): ["oprește tot în {x}", "stinge toate luminile din {x}"],
    ("ru", "ON"): ["включи всё в {x}", "включи всё освещение: {x}"],
    ("ru", "OFF"): ["выключи всё в {x}", "выключи всё освещение: {x}"],
}
QUERY_TEMPLATES = {
    "en": ["what is the {x}", "what does the {x} show", "tell me the {x}"],
    "ro": ["cât este {x}", "ce arată {x}", "spune-mi {x}"],
    "ru": ["какая {x}", "сколько показывает {x}", "скажи {x}"],
}
STATE_QUERY_TEMPLATES = {
    "en": ["is the {x} on?"], "ro": ["este pornit {x}?"], "ru": ["включен ли {x}?"],
}
SCENARIO_TEMPLATES = {
    "en": ["run {x}", "start the scenario {x}", "execute the automation {x}"],
    "ro": ["rulează {x}", "pornește scenariul {x}", "execută automatizarea {x}"],
    "ru": ["запусти сценарий {x}", "выполни {x}", "запусти автоматизацию {x}"],
}
MOWER = {
    "START": ["begin mowing", "mow the grass", "turn the mower on", "pornește robotul de tuns",
              "începe să tunzi iarba", "начни покос", "включи косилку"],
    "STOP": ["stop mowing", "stop the mower", "oprește robotul de tuns", "oprește tunsul",
             "останови косилку", "прекрати покос"],
    "PAUSE": ["pause the mower", "pause mowing", "pune pe pauză robotul de tuns", "поставь косилку на паузу"],
    "RESUME": ["resume mowing", "continue mowing", "reia tunsul", "continuă tunsul",
               "продолжи покос", "возобнови стрижку газона"],
    "DOCK": ["return the mower to its dock", "park the mower", "du robotul de tuns la stație",
             "parchează robotul de tuns", "верни косилку на станцию", "припаркуй косилку"],
}
CLARIFY = ["turn on the light", "switch off the lights", "turn on the heating", "open it",
           "aprinde lumina", "stinge lumina", "pornește încălzirea", "deschide-l",
           "включи свет", "выключи свет", "включи отопление", "открой это"]
OTHER = ["what's the weather tomorrow", "tell me a story", "who won the match", "how are you",
         "what time is it", "play some music", "ce vreme e mâine", "spune-mi o poveste",
         "cât e ceasul", "cine a câștigat meciul", "ce mai faci", "pune niște muzică",
         "какая завтра погода", "расскажи сказку", "который час", "как дела", "кто выиграл матч",
         "включи музыку", "set a timer for ten minutes", "what's on my calendar", "translate hello to german",
         "who is the president", "how far is the moon", "recommend a movie", "what's the news today",
         "thank you", "never mind", "pune o alarmă la șapte", "ce film îmi recomanzi", "ce știri sunt azi",
         "cât de departe e luna", "mulțumesc", "lasă", "поставь будильник на семь", "какие новости",
         "посоветуй фильм", "как далеко луна", "спасибо", "неважно", "переведи привет на немецкий"]

HEAT_WORD = {"en": "heating", "ro": "încălzirea", "ru": "отопление"}


def surfaces(entity, category=None):
    names = [entity["name"].strip()] + [a for a in entity.get("aliases") or [] if a]
    if category == "HEAT":
        names += [f"{HEAT_WORD[lang]} {entity['name'].strip()}" for lang in LANGS]
    return list(dict.fromkeys(n.lower() for n in names))


def kind_of(category):
    if category in LIGHT_CATS:
        return "light"
    if category in OPENABLE_CATS:
        return "open"
    return "other"


def build_training(catalog, augment=True):
    rows = build_rows(catalog)
    if augment:
        # Speech-to-text often drops Romanian diacritics; teach both spellings.
        rows += [(strip_accents(r[0]),) + r[1:] for r in rows if strip_accents(r[0]) != r[0]]
    return rows


def build_rows(catalog):
    rows = []

    def add(text, intent, target="none", action="none", scenario="none", mower="none"):
        rows.append((text, intent, target, action, scenario, mower))

    for p in catalog.get("peripherals") or []:
        key, cat = f"P{p['id']}", p.get("category")
        kind = kind_of(cat)
        for x in surfaces(p, cat):
            if cat not in SENSOR_CATS:
                for (lang, action), templates in VERBS.items():
                    for tpl, applies in templates:
                        if applies == "all" or applies == kind:
                            add(tpl.format(x=x), "control", key, action)
                for lang in LANGS:
                    for tpl in STATE_QUERY_TEMPLATES[lang]:
                        add(tpl.format(x=x), "query", key)
            else:
                for lang in LANGS:
                    for tpl in QUERY_TEMPLATES[lang]:
                        add(tpl.format(x=x), "query", key)
    for z in catalog.get("zones") or []:
        key = f"Z{z['id']}"
        for x in surfaces(z):
            for (lang, action), templates in ZONE_TEMPLATES.items():
                for tpl in templates:
                    add(tpl.format(x=x), "control", key, action)
    for s in catalog.get("scenarios") or []:
        key = f"S{s['jobId']}"
        for x in dict.fromkeys(v for v in (s.get("name"), s.get("description")) if v):
            for lang in LANGS:
                for tpl in SCENARIO_TEMPLATES[lang]:
                    add(tpl.format(x=x.lower()), "scenario", scenario=key)
    if catalog.get("mowers"):
        for action, texts in MOWER.items():
            for t in texts:
                add(t, "mower", mower=action)
    for t in CLARIFY:
        add(t, "clarify")
    for t in OTHER:
        add(t, "other")
    return rows
