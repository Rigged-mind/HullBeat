#!/usr/bin/env python3
"""WCAG 2.1 contrast check for every scheme in design/palette.json.

Run after ANY palette change. Exit 0 = all pairs pass.
Usage:  python tools/check_contrast.py
"""
import io
import json
import os
import sys

# --- palette --------------------------------------------------------------
# Read from design/palette.json, which is the single source. This file used to
# carry its own `P` dict under the comment "keep in sync with design/Color.kt",
# and the sync was a comment: the night scheme existed in the prototype's CSS
# for weeks and was never contrast-checked once, because adding a scheme there
# did not add it here.
PALETTE = json.load(io.open(os.path.join("design", "palette.json"),
                            encoding="utf-8"))
P = {name: s["tokens"] for name, s in PALETTE["schemes"].items()}
NEED = {name: s["contrast"] for name, s in PALETTE["schemes"].items()}
PAIRS = [tuple(row) for row in PALETTE["contrastPairs"]]

TEXT, UI = "text", "ui"


def luminance(hex_colour):
    h = hex_colour.lstrip("#")
    channels = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    channels = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
                for c in channels]
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]


def ratio(fg, bg):
    a, b = luminance(fg), luminance(bg)
    hi, lo = max(a, b), min(a, b)
    return (hi + 0.05) / (lo + 0.05)


def main():
    failures = []
    for scheme, tokens in P.items():
        print(f"\n{scheme.upper()}")
        print(f"  {'pair':42} {'ratio':>6}  need  verdict")
        print("  " + "-" * 66)
        for fg, bg, kind in PAIRS:
            need = NEED[scheme][kind]
            r = ratio(tokens[fg], tokens[bg])
            ok = r >= need
            if not ok:
                failures.append((scheme, fg, bg, r, need))
            label = f"{fg} on {bg}"
            print(f"  {label:42} {r:6.2f}  {need:4.1f}  "
                  f"{'PASS' if ok else '** FAIL **'}")

    print()
    if failures:
        print(f"FAILED: {len(failures)} pair(s) below threshold")
        for scheme, fg, bg, r, need in failures:
            print(f"  {scheme}: {fg} on {bg} = {r:.2f}, need {need}")
        return 1
    print("All pairs pass.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
