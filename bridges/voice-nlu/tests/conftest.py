import pytest
from sklearn.feature_extraction.text import HashingVectorizer

from voice_nlu.encoders import Profile

CATALOG = {
    "peripherals": [
        {"id": 1, "name": "Kitchen light", "category": "LIGHT", "zones": ["Kitchen"], "aliases": ["lumina bucătărie"]},
        {"id": 2, "name": "Garden valve", "category": "VALVE", "zones": ["Garden"], "aliases": ["robinet grădină"]},
        {"id": 3, "name": "Outside temperature", "category": "TEMP", "zones": [], "aliases": []},
    ],
    "zones": [{"id": 10, "name": "Kitchen", "peripherals": ["Kitchen light"], "aliases": ["bucătărie"]}],
    "scenarios": [{"jobId": 20, "name": "Evening lights", "description": "Turns on the evening lights"}],
    "mowers": [{"deviceId": 30, "name": "mower"}],
}


class StubEncoder:
    """Character n-gram hashing: deterministic, fast, separable enough for the tiny catalog."""
    model_file = "stub"

    def __init__(self):
        self.vec = HashingVectorizer(analyzer="char_wb", ngram_range=(2, 4), n_features=2048,
                                     alternate_sign=False, norm="l2")

    def encode(self, texts, batch_size=64):
        return self.vec.transform(texts).toarray().astype("float32")


@pytest.fixture
def catalog():
    return CATALOG


@pytest.fixture
def encoder():
    return StubEncoder()


@pytest.fixture
def profile():
    return Profile("stub", "stub", gate=0.9)
