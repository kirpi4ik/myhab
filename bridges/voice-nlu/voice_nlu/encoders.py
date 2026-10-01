"""Encoder profiles: the best measured variant of each candidate (docs/VOICE_PERFORMANCE_OPTIONS.md).

`gate` is the suggested confidence gate for myhab's feature.voice.nlu.gate; it is reported in
/health and not applied here.
"""
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Profile:
    name: str
    hf_id: str
    prefix: str = ""
    augment: bool = False
    balanced: bool = False
    gate: float = 0.9


PROFILES = {
    p.name: p for p in (
        Profile("minilm", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", gate=0.9),
        Profile("e5-small", "intfloat/multilingual-e5-small", prefix="query: ", augment=True, balanced=True, gate=0.6),
        Profile("e5-base", "intfloat/multilingual-e5-base", prefix="query: ", augment=True, balanced=True, gate=0.6),
    )
}


def profile(name):
    try:
        return PROFILES[name]
    except KeyError:
        raise ValueError(f"unknown encoder '{name}', expected one of {sorted(PROFILES)}") from None


def model_dir(name, models_dir, data_dir):
    """A model under <data>/models/<name> (a locally fine-tuned export) wins over the baked-in one."""
    for base in (Path(data_dir) / "models", Path(models_dir)):
        candidate = base / name
        if (candidate / "tokenizer.json").exists():
            return candidate
    raise FileNotFoundError(f"no exported model for '{name}' under {data_dir}/models or {models_dir}")
