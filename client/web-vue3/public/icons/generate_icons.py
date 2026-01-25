"""
Generate myHAB PWA icons from the source logo.

Goals:
- Improve visibility across devices/resolutions (especially 16/32px)
- Add a consistent background (works on light/dark launchers)
- Slightly thicken lines using alpha dilation before downscaling

Requires: Pillow (pip install Pillow)
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Iterable

from PIL import Image, ImageDraw, ImageFilter


SOURCE_LOGO = r"d:\download\chrome-personal\logo.png"
OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))

# Background chosen for maximum contrast with white mark
BACKGROUND_RGBA = (53, 62, 90, 255)  # #353e5a
MARK_RGBA = (255, 255, 255, 255)


@dataclass(frozen=True)
class IconSpec:
    filename: str
    size: int


ICONS: list[IconSpec] = [
    # Apple icons
    IconSpec("apple-icon-120x120.png", 120),
    IconSpec("apple-icon-152x152.png", 152),
    IconSpec("apple-icon-167x167.png", 167),
    IconSpec("apple-icon-180x180.png", 180),
    # General icons
    IconSpec("icon-128x128.png", 128),
    IconSpec("icon-192x192.png", 192),
    IconSpec("icon-256x256.png", 256),
    IconSpec("icon-384x384.png", 384),
    IconSpec("icon-512x512.png", 512),
    # Microsoft icon
    IconSpec("ms-icon-144x144.png", 144),
    # Favicons (png)
    IconSpec("favicon-16x16.png", 16),
    IconSpec("favicon-32x32.png", 32),
    IconSpec("favicon-96x96.png", 96),
    IconSpec("favicon-128x128.png", 128),
    # Main logo
    IconSpec("logo.png", 512),
]


def _padding_for_size(size: int) -> int:
    # For tiny sizes we want the mark to occupy more area.
    if size <= 32:
        return max(0, round(size * 0.02))
    if size <= 64:
        return max(1, round(size * 0.03))
    # Keep only a small margin so the logo's outer ring matches the icon circle.
    if size <= 128:
        return max(1, round(size * 0.02))
    return max(2, round(size * 0.015))


def _expand_bbox(bbox: tuple[int, int, int, int], px: int, max_w: int, max_h: int) -> tuple[int, int, int, int]:
    l, t, r, b = bbox
    l = max(0, l - px)
    t = max(0, t - px)
    r = min(max_w, r + px)
    b = min(max_h, b + px)
    return (l, t, r, b)


def _dilate_alpha(alpha: Image.Image, px: int) -> Image.Image:
    """Thicken strokes by dilating the alpha channel."""
    if px <= 0:
        return alpha
    # MaxFilter size must be odd.
    kernel = px * 2 + 1
    return alpha.filter(ImageFilter.MaxFilter(kernel))


def _alpha_from_saturation(base_rgb: Image.Image) -> Image.Image:
    """
    The provided logo file has an opaque baked-in checkerboard background
    (alpha channel is 255 everywhere). Extract a usable alpha mask by using
    the saturation channel: background is near-gray (low saturation) while
    the logo strokes are colored (high saturation).
    """
    hsv = base_rgb.convert("HSV")
    sat = hsv.split()[1]  # 0..255

    # Soft threshold saturation into an alpha channel.
    # - below t0 => fully transparent
    # - above t1 => fully opaque
    t0 = 18   # ~0.07
    t1 = 64   # ~0.25

    def _map(v: int) -> int:
        if v <= t0:
            return 0
        if v >= t1:
            return 255
        return int((v - t0) * 255 / (t1 - t0))

    return sat.point(_map)


def _render_icon(base_rgba: Image.Image, size: int) -> Image.Image:
    """
    Render an icon:
    - Convert logo to a white mark using its alpha channel
    - Slightly dilate alpha (more for tiny sizes)
    - Composite onto a solid background
    """
    if base_rgba.mode != "RGBA":
        base_rgba = base_rgba.convert("RGBA")

    # The source alpha is fully opaque; derive a usable mask from saturation.
    alpha = _alpha_from_saturation(base_rgba.convert("RGB"))

    # Thicken more aggressively at small sizes.
    if size <= 16:
        dilate_px = 3
    elif size <= 32:
        dilate_px = 2
    elif size <= 96:
        dilate_px = 1
    else:
        dilate_px = 0

    alpha = _dilate_alpha(alpha, dilate_px)

    # Build a white version of the logo using the derived alpha as mask.
    mark = Image.new("RGBA", base_rgba.size, (0, 0, 0, 0))
    mark_rgb = Image.new("RGBA", base_rgba.size, MARK_RGBA)
    mark = Image.composite(mark_rgb, mark, alpha)

    # Crop to the actual logo bounds (outer circle), so we can scale it up
    # to better match the icon circle.
    bbox = alpha.getbbox()
    if bbox:
        # Minimal expansion to avoid clipping the stroke, but keep it tight so
        # the rendered outer ring aligns closely with the icon circle.
        bbox = _expand_bbox(bbox, px=1, max_w=mark.width, max_h=mark.height)
        mark = mark.crop(bbox)

    pad = _padding_for_size(size)
    target_mark = max(1, size - pad * 2)

    # Resize the mark to target, then apply sharpening for crispness.
    mark_small = mark.resize((target_mark, target_mark), Image.LANCZOS)

    if size <= 32:
        mark_small = mark_small.filter(ImageFilter.UnsharpMask(radius=1, percent=300, threshold=2))
    elif size <= 128:
        mark_small = mark_small.filter(ImageFilter.UnsharpMask(radius=1, percent=220, threshold=3))

    # Composite onto a circular background with transparent outside.
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    # Small inset so the circle edge isn't clipped by antialiasing
    inset = 1 if size >= 32 else 0
    draw.ellipse((inset, inset, size - 1 - inset, size - 1 - inset), fill=BACKGROUND_RGBA)
    x = (size - target_mark) // 2
    y = (size - target_mark) // 2
    out.alpha_composite(mark_small, (x, y))
    return out


def generate_icons(specs: Iterable[IconSpec]) -> None:
    print(f"Loading source logo: {SOURCE_LOGO}")
    with Image.open(SOURCE_LOGO) as img:
        img = img.convert("RGBA")
        print(f"Source size: {img.size}, mode: {img.mode}")
        for spec in specs:
            rendered = _render_icon(img, spec.size)
            out_path = os.path.join(OUTPUT_DIR, spec.filename)
            rendered.save(out_path, "PNG", optimize=True)
            print(f"Wrote {spec.filename} ({spec.size}x{spec.size})")

        # Also generate favicon.ico (used by browsers, desktop bookmarks, etc.)
        # ICO is a multi-size container; we provide common sizes.
        ico_path = os.path.abspath(os.path.join(OUTPUT_DIR, "..", "favicon.ico"))
        ico_base = _render_icon(img, 256).convert("RGBA")
        ico_base = ico_base.filter(ImageFilter.UnsharpMask(radius=1, percent=180, threshold=2))
        ico_base.save(ico_path, format="ICO", sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
        print(f"Wrote favicon.ico (multi-size) -> {ico_path}")

    print("Done.")
    print("Note: safari-pinned-tab.svg is not regenerated by this script.")


if __name__ == "__main__":
    generate_icons(ICONS)

