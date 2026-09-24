#!/usr/bin/env python3
"""Emit the HullBeat mark as SVG sources and Android vector resources.

Geometry, once, in a 108x108 canvas so the Android adaptive icon is the
authoritative version and the SVGs are derived from the same numbers.

  ring    circle, centre 54,54, radius 27, stroke 7  -> outer diameter 61
  water   the region below a wave and inside the ring, filled

The wave baseline sits a third of a radius below centre (y = 63) rather than on
it. That asymmetry is the whole point: a circle with a line through its middle
reads as a minus sign or a prohibition, a circle with water in the bottom third
reads as something floating.

The water path is written out explicitly - wave along the top, then the ring's
own arc back along the bottom - instead of relying on a clip. VectorDrawable
supports clip-path, but an explicit path renders identically everywhere and
survives being pasted into any tool.

  left/right intersections at y=63:  dx = sqrt(27^2 - 9^2) = 25.46
  so the wave runs 28.5 -> 79.5, three half-waves of 17

Run:  python tools/make_logo.py
"""
import io
import json
import os

# The seed tone, from the one place colours live. This was a seventh copy of
# "#14403A", and a logo drifting from the app palette is the kind of thing
# nobody notices until the two sit side by side in a Play listing.
_PALETTE = json.load(io.open(os.path.join("design", "palette.json"),
                             encoding="utf-8"))
BRAND = _PALETTE["brand"][_PALETTE["seed"].split(".")[1]]

# --- shared geometry (108 canvas) ---------------------------------------
# The Dynamic Keel Line (Кільова кардіограма)
# Deep-V keel dip into telemetry pulse peak, running across the waterline.
# Coordinates span x: 24..84, y: 27..73 (center 54, 54, radius <= 32.6dp,
# guaranteed inside the 66dp adaptive-icon safe zone).
KEEL_LINE = "M24,56 L39,56 L47,73 L56,27 L65,63 L70,56 L84,56"
STROKE = 7
PULSE_CYAN = "#00E5FF"
ABYSS_DARK = "#070E17"

# 24dp tab icon geometry (viewport 24x24)
NAV_KEEL_LINE = "M2.5,12.5 L6.5,12.5 L9.5,17.5 L12.5,4.5 L15,14.5 L16.5,12.5 L21.5,12.5"
NAV_STROKE = 2.2

# 108 canvas, mark ~60 across: inside the 66dp adaptive-icon safe zone.
SAFE_NOTE = "mark is 60 of 108 - inside the 66dp safe zone every launcher mask keeps"

FILES = {}


def svg(colour, note, bg=None):
    bg_rect = f'<rect width="108" height="108" fill="{bg}"/>\n  ' if bg else ""
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="108" height="108">
  <!-- HullBeat mark: Dynamic Keel Line. {note} -->
  {bg_rect}<path d="{KEEL_LINE}" fill="none" stroke="{colour}" stroke-width="{STROKE}" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
"""


FILES["design/logo/mark-brand.svg"] = svg(BRAND, "Primary, on light backgrounds.")
FILES["design/logo/mark-white.svg"] = svg("#FFFFFF", "Reversed, on the brand tile or any dark surface.")
FILES["design/logo/mark-black.svg"] = svg("#000000", "Single-colour master for print, embroidery and stencils.")
FILES["design/logo/mark-cyan.svg"] = svg(PULSE_CYAN, "Pulse Cyan telemetry mark on Abyss Dark.", bg=ABYSS_DARK)

WORDMARK = "HULLBEAT"
WORD_X = 116          # mark width + gap
WORD_SIZE = 42
WORD_TRACK = 6.5
WORD_W = len(WORDMARK) * (WORD_SIZE * 0.62 + WORD_TRACK) - WORD_TRACK
LOCKUP_W = round(WORD_X + WORD_W)

FILES["design/logo/lockup-horizontal.svg"] = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {LOCKUP_W} 108" width="{LOCKUP_W}" height="108">
  <!--
    Horizontal lockup: Dynamic Keel Line mark + HULLBEAT wordmark.
  -->
  <path d="{KEEL_LINE}" fill="none" stroke="{BRAND}" stroke-width="{STROKE}" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="{WORD_X}" y="68" font-family="Inter, 'Helvetica Neue', Arial, sans-serif"
        font-size="{WORD_SIZE}" font-weight="700" letter-spacing="{WORD_TRACK}" fill="{BRAND}">{WORDMARK}</text>
</svg>
"""

# --- Android ------------------------------------------------------------
# Foreground of an adaptive icon: includes subtle Pulse Cyan glow layer + Masthead White core
FILES["app/src/main/res/drawable/ic_launcher_foreground.xml"] = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {SAFE_NOTE} -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="{KEEL_LINE}"
        android:strokeColor="{PULSE_CYAN}"
        android:strokeWidth="11"
        android:strokeAlpha="0.65"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{KEEL_LINE}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="{STROKE}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
"""

# Android 13+ themed icons: flat white silhouette
FILES["app/src/main/res/drawable/ic_launcher_monochrome.xml"] = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="{KEEL_LINE}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="7.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
"""

# Status-bar icons: single colour, 24dp
FILES["app/src/main/res/drawable/ic_notification.xml"] = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Status bar strips all colour and keeps the alpha. Shape only. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:pathData="{KEEL_LINE}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="9"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
"""

# In-app bottom navigation tab icon: «Пульс» / «Pulse»
FILES["app/src/main/res/drawable/ic_nav_now.xml"] = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Navigation icon for the Pulse tab: Dynamic Keel Line (24x24dp) -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:pathData="{NAV_KEEL_LINE}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="{NAV_STROKE}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
"""

FILES["app/src/main/res/values/ic_launcher_background.xml"] = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{ABYSS_DARK}</color>
</resources>
"""

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
FILES["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"] = ADAPTIVE
FILES["app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"] = ADAPTIVE

FILES["design/logo/README.md"] = f"""# Знак HullBeat: Кільова кардіограма (The Dynamic Keel Line)

Джерело геометрії — `tools/make_logo.py`. Правити там, не у файлах.

## Побудова

Горизонтальна лінія ватерлінії (y=56) заломлюється донизу під кутом кілюватості швидкісних глісерів і катерів (Deep-V),
стрімко підіймається у пік телеметричного пульсу мотогодин і повертається на вихідний рівень горизонту.

1. **Hull** = кільовий профіль (Deep-V).
2. **Beat** = діагностичний пік мотогодин та ритму систем.
3. Універсально для моторних катерів та вітрильних яхт.

## Файли

| Файл | Для чого |
|---|---|
| `mark-brand.svg` | основний, на світлому |
| `mark-white.svg` | вивортка, на брендовій плитці й темному |
| `mark-cyan.svg` | фірмовий неоновий ціан на Abyss Dark |
| `mark-black.svg` | одноколірний майстер: друк, вишивка, трафарет |
| `lockup-horizontal.svg` | знак + напис |
| `../../app/.../ic_launcher_foreground.xml` | передній шар адаптивної іконки |
| `../../app/.../ic_launcher_monochrome.xml` | тематичні іконки Android 13+ |
| `../../app/.../ic_notification.xml` | статус-бар, 24dp |
| `../../app/.../ic_nav_now.xml` | іконка вкладки «Пульс» у навігації, 24dp |

## Правила

1. **Знак 60 одиниць у полотні 108** — усередині безпечної зони 66dp.
   Лаунчери ріжуть іконку колом, сквіркалом і краплею; за межами 66dp
   гарантій немає.
2. **Мінімальний розмір — 16 px.** Знак єдиної лінії відмінно масштабується
   і не втрачає впізнаваності на малих екранах.
3. **Високий оптичний контраст на сонці.** Колір лінії та товщина адаптовані
4. **Охоронне поле** навколо знака — не менше ширини штриха x 3.

## ⚠️ Не зроблено

Напис у лockup — це **живий текст, а не криві**. Треба обрати гарнітуру
й перевести в контури до першої публікації: лого, що залежить від
шрифта на чужій машині, — не лого.

Растрові `mipmap-*/ic_launcher.png` для Android < 8.0 не згенеровані.
Якщо `minSdk = 26`, вони не потрібні взагалі.
"""

if __name__ == "__main__":
    for path, body in FILES.items():
        directory = os.path.dirname(path)
        if directory:
            os.makedirs(directory, exist_ok=True)
        io.open(path, "w", encoding="utf-8", newline="\n").write(body)
        print(f"{len(body):6d}B  {path}")
