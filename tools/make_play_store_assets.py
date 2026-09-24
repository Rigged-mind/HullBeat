#!/usr/bin/env python3
"""Generate Google Play Store graphical assets.

Outputs:
  design/playstore/icon-512.png
  design/playstore/feature-graphic-1024x500.png
"""
import io
import json
import math
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "design", "playstore")
os.makedirs(OUT_DIR, exist_ok=True)

# Load brand color
palette_path = os.path.join(ROOT, "design", "palette.json")
palette = json.load(io.open(palette_path, encoding="utf-8"))
BRAND = palette["brand"][palette["seed"].split(".")[1]]  # #14403A

ABYSS_DARK = (7, 14, 23)        # #070E17
PULSE_CYAN = (0, 229, 255)      # #00E5FF
WHITE = (255, 255, 255)

def draw_thick_polyline(draw, points, color, width):
    radius = width / 2.0
    for i in range(len(points) - 1):
        p1, p2 = points[i], points[i+1]
        draw.line([p1, p2], fill=color, width=round(width))
        draw.ellipse([p1[0] - radius, p1[1] - radius, p1[0] + radius, p1[1] + radius], fill=color)
        draw.ellipse([p2[0] - radius, p2[1] - radius, p2[0] + radius, p2[1] + radius], fill=color)

def make_icon():
    scale = 16
    size = 108 * scale
    img = Image.new("RGBA", (size, size), ABYSS_DARK)
    draw = ImageDraw.Draw(img)

    # Dynamic Keel Line points in 108x108 coordinate space
    pts_108 = [(24, 56), (39, 56), (47, 73), (56, 27), (65, 63), (70, 56), (84, 56)]
    pts = [(x * scale, y * scale) for x, y in pts_108]

    # Pulse Cyan glow underlay
    draw_thick_polyline(draw, pts, (0, 180, 210), width=11 * scale)
    # Crisp white core stroke
    draw_thick_polyline(draw, pts, WHITE, width=7 * scale)

    icon = img.resize((512, 512), Image.Resampling.LANCZOS)
    target = os.path.join(OUT_DIR, "icon-512.png")
    icon.convert("RGB").save(target, "PNG")
    print(f"Generated {target} ({os.path.getsize(target)} bytes)")

def make_feature_graphic():
    w, h = 1024, 500
    scale = 4
    sw, sh = w * scale, h * scale

    img = Image.new("RGB", (sw, sh), ABYSS_DARK)
    draw = ImageDraw.Draw(img)

    # Mark positioned elegantly on the left side
    m_scale = 22
    m_ox = round(sw * 0.20 - 54 * m_scale)
    m_oy = round(sh * 0.50 - 54 * m_scale)

    pts_108 = [(24, 56), (39, 56), (47, 73), (56, 27), (65, 63), (70, 56), (84, 56)]
    pts = [(m_ox + x * m_scale, m_oy + y * m_scale) for x, y in pts_108]

    draw_thick_polyline(draw, pts, (0, 180, 210), width=11 * m_scale)
    draw_thick_polyline(draw, pts, WHITE, width=7 * m_scale)

    font_path_bold = "C:/Windows/Fonts/segoeuib.ttf"
    font_path_reg = "C:/Windows/Fonts/segoeui.ttf"
    if os.path.exists(font_path_bold):
        title_font = ImageFont.truetype(font_path_bold, 68 * scale)
        sub_font = ImageFont.truetype(font_path_reg, 26 * scale)
    else:
        title_font = ImageFont.load_default()
        sub_font = ImageFont.load_default()

    text_x = round(sw * 0.46)
    text_y = round(sh * 0.35)

    draw.text((text_x, text_y), "HULLBEAT", font=title_font, fill="#FFFFFF")
    draw.text((text_x + 4 * scale, text_y + 84 * scale), "Boat Maintenance & Logbook", font=sub_font, fill="#00E5FF")
    draw.text((text_x + 4 * scale, text_y + 126 * scale), "100% Offline · Zero Tracking · Motor & Sail", font=sub_font, fill="#E0F2F1")

    feature = img.resize((w, h), Image.Resampling.LANCZOS)
    target = os.path.join(OUT_DIR, "feature-graphic-1024x500.png")
    feature.save(target, "PNG")
    print(f"Generated {target} ({os.path.getsize(target)} bytes)")

if __name__ == "__main__":
    make_icon()
    make_feature_graphic()

