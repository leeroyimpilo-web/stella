#!/usr/bin/env python3
import base64
import re
from pathlib import Path

FILES = [
    Path("app/src/main/java/com/stella/game/ui/ScenePixelData.kt"),
    Path("app/src/main/java/com/stella/game/ui/ItemPixelData.kt"),
]

failed = False
for path in FILES:
    text = path.read_text(encoding="utf-8")
    entries = re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', text)
    if not entries:
        print(f"ERROR: no artwork entries found in {path}")
        failed = True
        continue

    for key, encoded in entries:
        try:
            data = base64.b64decode(encoded, validate=True)
        except Exception as exc:
            print(f"ERROR: {path.name}:{key}: invalid base64: {exc}")
            failed = True
            continue

        if len(data) < 3:
            print(f"ERROR: {path.name}:{key}: payload shorter than header")
            failed = True
            continue

        width, height, colors = data[0], data[1], data[2]
        if width == 0 or height == 0 or colors == 0:
            print(f"ERROR: {path.name}:{key}: invalid dimensions/palette {width}x{height}, colors={colors}")
            failed = True
            continue

        expected = 3 + colors * 3 + width * height
        if len(data) != expected:
            print(
                f"ERROR: {path.name}:{key}: {len(data)} bytes, expected {expected} "
                f"({width}x{height}, colors={colors})"
            )
            failed = True
            continue

        pixel_offset = 3 + colors * 3
        max_index = max(data[pixel_offset:], default=0)
        if max_index >= colors:
            print(
                f"ERROR: {path.name}:{key}: pixel palette index {max_index} "
                f"exceeds palette size {colors}"
            )
            failed = True
            continue

        print(f"OK: {path.name}:{key} {width}x{height} colors={colors}")

if failed:
    raise SystemExit(1)

print("All embedded STELLA artwork payloads are structurally valid.")
