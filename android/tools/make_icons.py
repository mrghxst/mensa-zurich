# SPDX-License-Identifier: GPL-3.0-or-later
"""Generate the adaptive launcher icon layers from ethmensa/App/AppIcon.icon.

Usage (from the repository root, needs Pillow):
    python android/tools/make_icons.py

The iOS icon is a full-bleed 1024 px square with a white glyph (front.png) on a
#215CAF -> #08407E gradient. Android adaptive icons are 108 dp with a 72 dp
visible area, so the iOS canvas maps to the central 72 dp of the 108 dp layer.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
FRONT = ROOT / "ethmensa" / "App" / "AppIcon.icon" / "Assets" / "front.png"
FULL = ROOT / "ethmensa" / "App" / "Assets.xcassets" / "AppIcon.appiconset" / "ethmensa.png"
RES = ROOT / "android" / "app" / "src" / "main" / "res"
DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}


def foreground(size: int) -> Image.Image:
    front = Image.open(FRONT).convert("RGBA")
    inner = round(size * 72 / 108)
    glyph = front.resize((inner, inner), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - inner) // 2
    canvas.alpha_composite(glyph, (offset, offset))
    return canvas


def main() -> None:
    for density, scale in DENSITIES.items():
        size = round(108 * scale)
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        foreground(size).save(folder / "ic_launcher_foreground.png", optimize=True)
        print(f"wrote {folder.name}/ic_launcher_foreground.png ({size}px)")

    # Rounded app icon for onboarding, About and the list fallback thumbnail (78 dp at xxxhdpi).
    nodpi = RES / "drawable-nodpi"
    nodpi.mkdir(parents=True, exist_ok=True)
    Image.open(FULL).convert("RGB").resize((320, 320), Image.LANCZOS).save(nodpi / "app_icon.png", optimize=True)
    print("wrote drawable-nodpi/app_icon.png")


if __name__ == "__main__":
    main()
