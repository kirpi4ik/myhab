import pytest

from voice_nlu.lexicon import lexicon_action


@pytest.mark.parametrize("text,action", [
    ("Turn off the kitchen light", "OFF"),
    ("turn of the attic lite", "OFF"),          # misheard "off"
    ("Close the main water valve", "OFF"),
    ("Stinge lumina din pod", "OFF"),
    ("Închide robinetul de apă", "OFF"),
    ("inchide robinetul", "OFF"),               # no diacritics
    ("Выключи свет на кухне", "OFF"),
    ("Перекрой воду в доме", "OFF"),
    ("Turn on the office light", "ON"),
    ("Aprinde lumina în birou", "ON"),
    ("Dă drumul la apa de afară", "ON"),
    ("Включи свет", "ON"),
    ("Открой калитку", "ON"),
    ("Toggle the dressing room light", "TOGGLE"),
    ("Comută lumina din dressing", "TOGGLE"),
    ("Переключи свет в гардеробной", "TOGGLE"),
])
def test_verbs(text, action):
    assert lexicon_action(text) == action


@pytest.mark.parametrize("text", ["Restart the outdoor router", "Water the lawn", "What is the temperature"])
def test_no_verb(text):
    assert lexicon_action(text) is None
