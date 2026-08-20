#!/usr/bin/env python3
"""Inspect user-provided game assets without changing pixels."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
INPUT_DIR = ROOT / "res/assets/incoming/user_uploads"
GENERATED_DIR = ROOT / "res/assets/generated"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()[:16]


def inspect(path: Path) -> dict[str, Any]:
    with Image.open(path) as image:
        rgba = image.convert("RGBA")
        width, height = rgba.size
        alpha = rgba.getchannel("A")
        alpha_min, alpha_max = alpha.getextrema()
        alpha_hist = alpha.histogram()
        opaque = sum(alpha_hist[250:])
        nontransparent = sum(alpha_hist[1:])
        bbox = alpha.getbbox()
        colors = rgba.getcolors(maxcolors=width * height + 1)
        unique_colors = len(colors) if colors is not None else None
        return {
            "file": path.name,
            "format": image.format,
            "mode": image.mode,
            "width": width,
            "height": height,
            "sha256_16": sha256(path),
            "alpha_min": alpha_min,
            "alpha_max": alpha_max,
            "nontransparent_pixels": nontransparent,
            "nontransparent_ratio": round(nontransparent / (width * height), 6),
            "opaque_pixels": opaque,
            "opaque_ratio": round(opaque / (width * height), 6),
            "alpha_bbox": list(bbox) if bbox else None,
            "unique_colors": unique_colors,
        }


def main() -> None:
    uploads = sorted(p for p in INPUT_DIR.iterdir() if p.suffix.lower() in {".png", ".webp", ".jpg", ".jpeg"})
    generated = {p.name: sha256(p) for p in GENERATED_DIR.rglob("*") if p.is_file() and p.suffix.lower() == ".png"}
    report = {
        "input_dir": str(INPUT_DIR),
        "generated_png_hashes": generated,
        "assets": [inspect(path) for path in uploads],
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
