"""Export the encoder profiles to int8 ONNX: <out>/<name>/{model_quantized.onnx, tokenizer.json}.

Runs in the image's build stage (needs optimum + torch, which the runtime image does not ship)
and locally to prepare models for a non-Docker run:

    python export_models.py --out models                 # all profiles
    python export_models.py --out models --names minilm
"""
import argparse
import shutil
import tempfile
from pathlib import Path

from voice_nlu.encoders import PROFILES


def export(name, out_dir):
    from optimum.onnxruntime import ORTModelForFeatureExtraction, ORTQuantizer
    from optimum.onnxruntime.configuration import AutoQuantizationConfig
    from transformers import AutoTokenizer

    prof = PROFILES[name]
    target = Path(out_dir) / name
    target.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        ORTModelForFeatureExtraction.from_pretrained(prof.hf_id, export=True).save_pretrained(tmp)
        # Dynamic int8 for AVX2 CPUs (any recent x86 server, including the i5 class).
        ORTQuantizer.from_pretrained(tmp).quantize(
            save_dir=target, quantization_config=AutoQuantizationConfig.avx2(is_static=False, per_channel=False))
    AutoTokenizer.from_pretrained(prof.hf_id).save_pretrained(target)
    for f in target.iterdir():
        if f.name not in ("model_quantized.onnx", "tokenizer.json"):
            shutil.rmtree(f) if f.is_dir() else f.unlink()
    print(f"{name}: {target / 'model_quantized.onnx'} ({(target / 'model_quantized.onnx').stat().st_size / 1e6:.0f} MB)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="/models")
    ap.add_argument("--names", nargs="*", default=sorted(PROFILES))
    args = ap.parse_args()
    for name in args.names:
        export(name, args.out)


if __name__ == "__main__":
    main()
