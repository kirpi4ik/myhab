"""Sentence embeddings with ONNX Runtime: tokenizer -> transformer -> mean pooling -> L2 norm.

Both candidate families (paraphrase-multilingual, multilingual-e5) are mean-pooled
sentence-transformers models, so this reproduces their `encode(..., normalize_embeddings=True)`.
"""
from pathlib import Path

import numpy as np


class OnnxEncoder:
    def __init__(self, model_dir, prefix="", threads=2, max_len=128):
        import onnxruntime as ort
        from tokenizers import Tokenizer

        model_dir = Path(model_dir)
        onnx_file = next((model_dir / f for f in ("model_quantized.onnx", "model.onnx")
                          if (model_dir / f).exists()), None)
        if onnx_file is None:
            raise FileNotFoundError(f"no model.onnx in {model_dir}")
        opts = ort.SessionOptions()
        opts.intra_op_num_threads = threads
        opts.inter_op_num_threads = 1
        self.session = ort.InferenceSession(str(onnx_file), opts, providers=["CPUExecutionProvider"])
        self.input_names = {i.name for i in self.session.get_inputs()}
        self.tokenizer = Tokenizer.from_file(str(model_dir / "tokenizer.json"))
        self.tokenizer.enable_truncation(max_len)
        self.tokenizer.enable_padding()
        self.prefix = prefix
        self.model_file = onnx_file.name

    def encode(self, texts, batch_size=64):
        out = []
        for start in range(0, len(texts), batch_size):
            batch = self.tokenizer.encode_batch([self.prefix + t for t in texts[start:start + batch_size]])
            ids = np.array([e.ids for e in batch], dtype=np.int64)
            mask = np.array([e.attention_mask for e in batch], dtype=np.int64)
            feeds = {"input_ids": ids, "attention_mask": mask}
            if "token_type_ids" in self.input_names:
                feeds["token_type_ids"] = np.zeros_like(ids)
            hidden = self.session.run(None, feeds)[0]
            m = mask[..., None].astype(np.float32)
            pooled = (hidden * m).sum(axis=1) / np.clip(m.sum(axis=1), 1e-9, None)
            out.append(pooled / np.clip(np.linalg.norm(pooled, axis=1, keepdims=True), 1e-12, None))
        return np.vstack(out).astype(np.float32)
