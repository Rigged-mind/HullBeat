#!/usr/bin/env python3
"""Generate high-resolution device mockup screenshots for HullBeat documentation.

Generates 8 screenshots (4 Ukrainian, 4 English):
  - Pulse (Now) screen
  - Maintenance Journal with photo indicators
  - Parts Store & Locker Inventory
  - Night Navigation Mode
"""

import os
from PIL import Image, ImageDraw, ImageFont

OUT_DIR = os.path.join("docs", "screenshots")
os.makedirs(OUT_DIR, exist_ok=True)

# 2x supersampling for ultra-crisp text & curves
W, H = 820, 1720  # renders to 410x860 display
SCALE = 2

FONT_REG = r"C:\Windows\Fonts\segoeui.ttf"
FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_SEMI = r"C:\Windows\Fonts\seguisb.ttf"
if not os.path.exists(FONT_SEMI):
    FONT_SEMI = FONT_BOLD

def get_font(size, bold=False, semi=False):
    path = FONT_BOLD if bold else (FONT_SEMI if semi else FONT_REG)
    return ImageFont.truetype(path, round(size * SCALE))

# Color Palettes
# Standard Light Theme
C_BG = (247, 250, 249)
C_SURFACE = (255, 255, 255)
C_VARIANT = (230, 240, 237)
C_INK = (11, 31, 28)
C_MUTED = (74, 95, 91)
C_PRIMARY = (20, 64, 58)      # #14403A
C_ON_PRIMARY = (255, 255, 255)
C_DIVIDER = (211, 224, 220)
C_OUTLINE = (110, 135, 129)
C_OVERDUE = (179, 38, 30)     # #B3261E
C_TINT_OVERDUE = (251, 228, 226)
C_SOON = (138, 82, 0)         # #8A5200
C_TINT_SOON = (253, 240, 220)
C_OK = (46, 108, 96)
C_TINT_OK = (230, 240, 237)

# Night Navigation Theme
N_BG = (8, 3, 2)              # #080302
N_SURFACE = (15, 6, 4)        # #0F0604
N_VARIANT = (28, 10, 6)       # #1C0A06
N_PRIMARY = (232, 84, 58)     # #E8543A
N_ON_PRIMARY = (10, 3, 1)
N_INK = (232, 84, 58)
N_MUTED = (209, 74, 50)
N_DIVIDER = (42, 15, 9)
N_OVERDUE = (255, 122, 85)
N_TINT_OVERDUE = (46, 14, 7)
N_SOON = (232, 84, 58)
N_TINT_SOON = (36, 11, 5)

def draw_device_base(draw, is_night=False):
    bg = N_BG if is_night else C_BG
    draw.rectangle([0, 0, W, H], fill=bg)

def draw_status_bar(draw, is_night=False):
    color = N_MUTED if is_night else C_MUTED
    f_time = get_font(13, semi=True)
    f_icons = get_font(12, bold=True)
    draw.text((28 * SCALE, 14 * SCALE), "10:42", fill=color, font=f_time)
    draw.text((W - 85 * SCALE, 14 * SCALE), "LTE  100%", fill=color, font=f_icons)

def draw_bottom_nav(draw, active_tab=0, is_night=False, lang="uk"):
    y0 = H - 76 * SCALE
    bg = N_SURFACE if is_night else C_SURFACE
    divider = N_DIVIDER if is_night else C_DIVIDER
    muted = N_MUTED if is_night else C_MUTED
    primary = N_PRIMARY if is_night else C_PRIMARY
    
    draw.rectangle([0, y0, W, H], fill=bg)
    draw.line([0, y0, W, y0], fill=divider, width=max(1, round(1 * SCALE)))
    
    tabs_uk = ["Пульс", "Журнал", "Судно", "Склад"]
    tabs_en = ["Pulse", "Journal", "Vessel", "Store"]
    tabs = tabs_uk if lang == "uk" else tabs_en
    
    col_w = W // 4
    f_lbl = get_font(11, semi=True)
    
    for i, lbl in enumerate(tabs):
        cx = i * col_w + col_w // 2
        cy = y0 + 22 * SCALE
        is_active = (i == active_tab)
        txt_color = primary if is_active else muted
        
        # Draw custom vector icon for each tab
        if i == 0:
            # Pulse / ECG wave
            pts = [
                (cx - 13 * SCALE, cy),
                (cx - 6 * SCALE, cy),
                (cx - 2 * SCALE, cy - 9 * SCALE),
                (cx + 3 * SCALE, cy + 9 * SCALE),
                (cx + 7 * SCALE, cy - 4 * SCALE),
                (cx + 10 * SCALE, cy),
                (cx + 13 * SCALE, cy)
            ]
            draw.line(pts, fill=txt_color, width=2 * SCALE)
        elif i == 1:
            # Clipboard / Journal
            draw.rounded_rectangle([cx - 8 * SCALE, cy - 10 * SCALE, cx + 8 * SCALE, cy + 10 * SCALE],
                                   radius=2 * SCALE, outline=txt_color, width=2 * SCALE)
            draw.line([cx - 4 * SCALE, cy - 10 * SCALE, cx + 4 * SCALE, cy - 10 * SCALE],
                      fill=txt_color, width=3 * SCALE)
            draw.line([cx - 4 * SCALE, cy - 3 * SCALE, cx + 4 * SCALE, cy - 3 * SCALE],
                      fill=txt_color, width=2 * SCALE)
            draw.line([cx - 4 * SCALE, cy + 2 * SCALE, cx + 4 * SCALE, cy + 2 * SCALE],
                      fill=txt_color, width=2 * SCALE)
            draw.line([cx - 4 * SCALE, cy + 6 * SCALE, cx + 1 * SCALE, cy + 6 * SCALE],
                      fill=txt_color, width=2 * SCALE)
        elif i == 2:
            # Sailboat
            draw.polygon([
                (cx - 12 * SCALE, cy + 6 * SCALE),
                (cx + 12 * SCALE, cy + 6 * SCALE),
                (cx + 8 * SCALE, cy + 11 * SCALE),
                (cx - 8 * SCALE, cy + 11 * SCALE)
            ], fill=txt_color)
            draw.line([cx - 2 * SCALE, cy - 11 * SCALE, cx - 2 * SCALE, cy + 5 * SCALE],
                      fill=txt_color, width=2 * SCALE)
            draw.polygon([
                (cx - 3 * SCALE, cy - 10 * SCALE),
                (cx - 3 * SCALE, cy + 4 * SCALE),
                (cx - 11 * SCALE, cy + 4 * SCALE)
            ], fill=txt_color)
            draw.polygon([
                (cx, cy - 8 * SCALE),
                (cx, cy + 4 * SCALE),
                (cx + 9 * SCALE, cy + 4 * SCALE)
            ], fill=txt_color)
        elif i == 3:
            # Inventory box
            draw.rounded_rectangle([cx - 9 * SCALE, cy - 8 * SCALE, cx + 9 * SCALE, cy + 9 * SCALE],
                                   radius=2 * SCALE, outline=txt_color, width=2 * SCALE)
            draw.line([cx - 9 * SCALE, cy - 2 * SCALE, cx + 9 * SCALE, cy - 2 * SCALE],
                      fill=txt_color, width=2 * SCALE)
            draw.line([cx, cy - 2 * SCALE, cx, cy + 9 * SCALE],
                      fill=txt_color, width=2 * SCALE)
            
        # Label
        draw.text((cx, y0 + 40 * SCALE), lbl, fill=txt_color, font=f_lbl, anchor="mt")
        
        if is_active:
            pill_w = 48 * SCALE
            draw.rounded_rectangle([cx - pill_w // 2, y0 + 60 * SCALE, cx + pill_w // 2, y0 + 64 * SCALE],
                                   radius=2 * SCALE, fill=primary)

def draw_top_app_bar(draw, vessel="Stella Maris", model="Bavaria 38", is_night=False):
    y0 = 36 * SCALE
    ink = N_INK if is_night else C_INK
    muted = N_MUTED if is_night else C_MUTED
    
    f_vessel = get_font(18, bold=True)
    f_model = get_font(13)
    
    # Vessel title
    draw.text((20 * SCALE, y0 + 4 * SCALE), vessel, fill=ink, font=f_vessel)
    vw = draw.textlength(vessel, font=f_vessel)
    # Down caret triangle
    vx = 20 * SCALE + vw + 8 * SCALE
    vy = y0 + 16 * SCALE
    draw.polygon([(vx, vy), (vx + 10 * SCALE, vy), (vx + 5 * SCALE, vy + 6 * SCALE)], fill=ink)
    
    # Subtitle
    draw.text((20 * SCALE, y0 + 28 * SCALE), model, fill=muted, font=f_model)
    
    # Search icon (magnifier)
    sx = W - 68 * SCALE
    sy = y0 + 12 * SCALE
    r = 6 * SCALE
    draw.ellipse([sx - r, sy - r, sx + r, sy + r], outline=muted, width=2 * SCALE)
    draw.line([sx + 4 * SCALE, sy + 4 * SCALE, sx + 10 * SCALE, sy + 10 * SCALE], fill=muted, width=2 * SCALE)
    
    # More menu (3 vertical dots)
    mx = W - 28 * SCALE
    for dy in (-7, 0, 7):
        draw.ellipse([mx - 2 * SCALE, sy + dy * SCALE - 2 * SCALE,
                      mx + 2 * SCALE, sy + dy * SCALE + 2 * SCALE], fill=muted)


# =========================================================================
# SCREEN 1: NOW / PULSE
# =========================================================================
def make_now_screen(lang="uk", is_night=False):
    img = Image.new("RGB", (W, H))
    draw = ImageDraw.Draw(img)
    draw_device_base(draw, is_night)
    draw_status_bar(draw, is_night)
    draw_top_app_bar(draw, is_night=is_night)
    draw_bottom_nav(draw, active_tab=0, is_night=is_night, lang=lang)
    
    y = 96 * SCALE
    
    # Status Banner
    banner_bg = N_TINT_OVERDUE if is_night else C_TINT_OVERDUE
    banner_overdue = N_OVERDUE if is_night else C_OVERDUE
    banner_soon = N_SOON if is_night else C_SOON
    f_stat = get_font(13, semi=True)
    
    draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + 36 * SCALE], radius=10 * SCALE, fill=banner_bg)
    
    # Overdue dot & text
    bx = 28 * SCALE
    by = y + 18 * SCALE
    draw.ellipse([bx - 4 * SCALE, by - 4 * SCALE, bx + 4 * SCALE, by + 4 * SCALE], fill=banner_overdue)
    bx += 10 * SCALE
    
    txt_overdue = "2 прострочено" if lang == "uk" else "2 overdue"
    draw.text((bx, by), txt_overdue, fill=banner_overdue, font=f_stat, anchor="lm")
    bx += draw.textlength(txt_overdue, font=f_stat) + 12 * SCALE
    
    # Divider dot
    draw.ellipse([bx - 2 * SCALE, by - 2 * SCALE, bx + 2 * SCALE, by + 2 * SCALE], fill=banner_overdue)
    bx += 14 * SCALE
    
    # Soon dot & text
    draw.ellipse([bx - 4 * SCALE, by - 4 * SCALE, bx + 4 * SCALE, by + 4 * SCALE], fill=banner_soon)
    bx += 10 * SCALE
    txt_soon = "1 підходить скоро" if lang == "uk" else "1 due soon"
    draw.text((bx, by), txt_soon, fill=banner_soon, font=f_stat, anchor="lm")
    
    y += 48 * SCALE
    
    # Card 1: Impeller (Overdue)
    title1 = "Імпелер помпи забортної води" if lang == "uk" else "Raw-water pump impeller"
    sub1 = "Головний двигун (Volvo D2-55)" if lang == "uk" else "Main Engine (Volvo D2-55)"
    metric1 = "248 з 250 мотогодин" if lang == "uk" else "248 of 250 engine hours"
    btn1 = "Зроблено" if lang == "uk" else "Done"
    defer1 = "Не можу зараз — відкласти" if lang == "uk" else "Cannot do now — defer"
    
    y = draw_now_card(draw, y, title1, sub1, metric1, btn1, defer1,
                      dot_color=N_OVERDUE if is_night else C_OVERDUE,
                      is_night=is_night)
    
    # Card 2: Anodes (Overdue days)
    title2 = "Корпусні та валові аноди (цинк)" if lang == "uk" else "Sacrificial zinc anodes (hull & shaft)"
    sub2 = "Корпус і підводна частина" if lang == "uk" else "Hull & Underwater gear"
    metric2 = "Прострочено на 14 днів" if lang == "uk" else "Overdue by 14 days"
    btn2 = "Зроблено" if lang == "uk" else "Done"
    defer2 = "Відкласти до підйому" if lang == "uk" else "Defer to haul-out"
    
    y = draw_now_card(draw, y, title2, sub2, metric2, btn2, defer2,
                      dot_color=N_OVERDUE if is_night else C_OVERDUE,
                      is_night=is_night)
    
    # Card 3: Rigging (Due soon)
    title3 = "Стоячий такелаж і талрепи" if lang == "uk" else "Standing rigging & turnbuckles"
    sub3 = "Вітрила та такелаж" if lang == "uk" else "Sails & Rigging"
    metric3 = "Плановий огляд: через 18 днів" if lang == "uk" else "Inspection due in 18 days"
    btn3 = "Оглянуто" if lang == "uk" else "Inspect"
    defer3 = ""
    
    y = draw_now_card(draw, y, title3, sub3, metric3, btn3, defer3,
                      dot_color=N_SOON if is_night else C_SOON,
                      is_night=is_night)
    
    return img

def draw_now_card(draw, y, title, sub, metric, btn_txt, defer_txt, dot_color, is_night=False):
    card_h = 138 * SCALE if defer_txt else 116 * SCALE
    card_bg = N_SURFACE if is_night else C_SURFACE
    card_line = N_DIVIDER if is_night else C_DIVIDER
    ink = N_INK if is_night else C_INK
    muted = N_MUTED if is_night else C_MUTED
    btn_bg = N_PRIMARY if is_night else C_PRIMARY
    btn_fg = N_ON_PRIMARY if is_night else C_ON_PRIMARY
    
    draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + card_h],
                           radius=14 * SCALE, fill=card_bg, outline=card_line, width=max(1, round(1 * SCALE)))
    
    # Dot
    draw.ellipse([26 * SCALE, y + 19 * SCALE, 34 * SCALE, y + 27 * SCALE], fill=dot_color)
    
    # Titles
    draw.text((42 * SCALE, y + 14 * SCALE), title, fill=ink, font=get_font(15, bold=True))
    draw.text((42 * SCALE, y + 36 * SCALE), sub, fill=muted, font=get_font(12))
    draw.text((42 * SCALE, y + 54 * SCALE), metric, fill=dot_color, font=get_font(12, semi=True))
    
    # 56dp Action button
    btn_w, btn_btn_h = 110 * SCALE, 50 * SCALE
    bx = W - 26 * SCALE - btn_w
    by = y + 20 * SCALE
    draw.rounded_rectangle([bx, by, bx + btn_w, by + btn_btn_h], radius=12 * SCALE, fill=btn_bg)
    draw.text((bx + btn_w // 2, by + btn_btn_h // 2), btn_txt, fill=btn_fg, font=get_font(14, bold=True), anchor="mm")
    
    # Defer link
    if defer_txt:
        draw.text((bx + btn_w // 2, y + 84 * SCALE), defer_txt, fill=muted, font=get_font(10, semi=True), anchor="mm")
    
    return y + card_h + 12 * SCALE


# =========================================================================
# SCREEN 2: JOURNAL
# =========================================================================
def make_journal_screen(lang="uk"):
    img = Image.new("RGB", (W, H))
    draw = ImageDraw.Draw(img)
    draw_device_base(draw, False)
    draw_status_bar(draw, False)
    draw_top_app_bar(draw)
    draw_bottom_nav(draw, active_tab=1, lang=lang)
    
    y = 96 * SCALE
    
    # Chips row
    chips_uk = ["Всі", "Планове ТО", "Ремонт", "Інспекція"]
    chips_en = ["All", "Scheduled", "Repair", "Inspection"]
    chips = chips_uk if lang == "uk" else chips_en
    
    cx = 16 * SCALE
    for i, ch in enumerate(chips):
        bg = C_PRIMARY if i == 0 else C_VARIANT
        fg = C_ON_PRIMARY if i == 0 else C_INK
        txt_font = get_font(12, semi=True)
        tw = draw.textlength(ch, font=txt_font)
        pw = tw + 20 * SCALE
        draw.rounded_rectangle([cx, y, cx + pw, y + 32 * SCALE], radius=16 * SCALE, fill=bg)
        draw.text((cx + pw // 2, y + 16 * SCALE), ch, fill=fg, font=txt_font, anchor="mm")
        cx += pw + 8 * SCALE
        
    y += 44 * SCALE
    
    # Summary banner
    sum_txt = "38 робіт у журналі  ·  Витрати за сезон: 1 450 €" if lang == "uk" else "38 entries in log  ·  Season spend: €1,450"
    draw.text((18 * SCALE, y), sum_txt, fill=C_MUTED, font=get_font(12, semi=True))
    y += 24 * SCALE
    
    # Records
    recs_uk = [
        ("24 ВЕРЕСНЯ 2026", "Заміна моторної оливи та масляного фільтра", "Volvo D2-55 · 15W-40 VDS-3 (4.5 л)", "Планове ТО", "120 €", "2 фото"),
        ("18 СЕРПНЯ 2026", "Заміна крильчатки забортної помпи", "Johnson 09-1027B · плановий сервіс", "Планове ТО", "45 €", "1 фото"),
        ("04 ЛИПНЯ 2026", "Ревізія та змащення лебідок кокпіта", "Harken 40 ST · розбирання, собачки, мастило", "Інспекція", "30 €", ""),
        ("15 ТРАВНЯ 2026", "Заміна цинкових анодів сейлдвайва", "Saildrive 130S · кільцевий цинк", "Планове ТО", "65 €", "1 фото"),
    ]
    recs_en = [
        ("24 SEPTEMBER 2026", "Engine oil & filter change", "Volvo D2-55 · 15W-40 VDS-3 (4.5 L)", "Scheduled", "€120", "2 photos"),
        ("18 AUGUST 2026", "Raw water pump impeller replacement", "Johnson 09-1027B · scheduled service", "Scheduled", "€45", "1 photo"),
        ("04 JULY 2026", "Cockpit winches strip & grease", "Harken 40 ST · pawls, springs, marine grease", "Inspection", "€30", ""),
        ("15 MAY 2026", "Saildrive zinc anode renewal", "Saildrive 130S · collar zinc kit", "Scheduled", "€65", "1 photo"),
    ]
    recs = recs_uk if lang == "uk" else recs_en
    
    for date_hdr, what, desc, tag, cost, photo_badge in recs:
        # Date header
        draw.text((18 * SCALE, y), date_hdr, fill=C_MUTED, font=get_font(10, bold=True))
        y += 18 * SCALE
        
        card_h = 92 * SCALE
        draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + card_h],
                                radius=12 * SCALE, fill=C_SURFACE, outline=C_DIVIDER, width=max(1, round(1 * SCALE)))
        
        draw.text((28 * SCALE, y + 14 * SCALE), what, fill=C_INK, font=get_font(14, bold=True))
        draw.text((28 * SCALE, y + 36 * SCALE), desc, fill=C_MUTED, font=get_font(12))
        
        # Tags row
        bx = 28 * SCALE
        # Type tag
        tw = draw.textlength(tag, font=get_font(10, semi=True)) + 14 * SCALE
        draw.rounded_rectangle([bx, y + 58 * SCALE, bx + tw, y + 78 * SCALE], radius=6 * SCALE, fill=C_VARIANT)
        draw.text((bx + tw // 2, y + 68 * SCALE), tag, fill=C_PRIMARY, font=get_font(10, semi=True), anchor="mm")
        bx += tw + 8 * SCALE
        
        # Cost tag
        cw = draw.textlength(cost, font=get_font(10, semi=True)) + 14 * SCALE
        draw.rounded_rectangle([bx, y + 58 * SCALE, bx + cw, y + 78 * SCALE], radius=6 * SCALE, fill=C_VARIANT)
        draw.text((bx + cw // 2, y + 68 * SCALE), cost, fill=C_MUTED, font=get_font(10, semi=True), anchor="mm")
        bx += cw + 8 * SCALE
        
        # Photo badge with custom vector camera icon
        if photo_badge:
            f_photo = get_font(10, semi=True)
            txt_w = draw.textlength(photo_badge, font=f_photo)
            badge_w = txt_w + 30 * SCALE
            draw.rounded_rectangle([bx, y + 58 * SCALE, bx + badge_w, y + 78 * SCALE], radius=6 * SCALE, fill=C_TINT_OK)
            
            # Draw vector camera icon
            cx = bx + 9 * SCALE
            cy = y + 68 * SCALE
            draw.rounded_rectangle([cx - 5 * SCALE, cy - 4 * SCALE, cx + 5 * SCALE, cy + 4 * SCALE],
                                   radius=1 * SCALE, outline=C_OK, width=1 * SCALE)
            draw.line([cx - 2 * SCALE, cy - 5 * SCALE, cx + 2 * SCALE, cy - 5 * SCALE], fill=C_OK, width=1 * SCALE)
            draw.ellipse([cx - 2 * SCALE, cy - 2 * SCALE, cx + 2 * SCALE, cy + 2 * SCALE], fill=C_OK)
            
            draw.text((bx + 18 * SCALE, y + 68 * SCALE), photo_badge, fill=C_OK, font=f_photo, anchor="lm")
            
        y += card_h + 12 * SCALE
        
    return img


# =========================================================================
# SCREEN 3: STORE / INVENTORY
# =========================================================================
def make_store_screen(lang="uk"):
    img = Image.new("RGB", (W, H))
    draw = ImageDraw.Draw(img)
    draw_device_base(draw, False)
    draw_status_bar(draw, False)
    draw_top_app_bar(draw)
    draw_bottom_nav(draw, active_tab=3, lang=lang)
    
    y = 96 * SCALE
    
    # 56dp QR Scan Button with vector QR icon
    btn_lbl = "Сканувати QR рундука / деталі" if lang == "uk" else "Scan Locker QR / Barcode"
    f_btn = get_font(14, bold=True)
    draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + 54 * SCALE],
                           radius=14 * SCALE, fill=C_PRIMARY)
    
    # Vector QR icon
    qx = 42 * SCALE
    qy = y + 27 * SCALE
    s = 8 * SCALE
    draw.rectangle([qx - s, qy - s, qx + s, qy + s], outline=C_ON_PRIMARY, width=2 * SCALE)
    draw.rectangle([qx - 4 * SCALE, qy - 4 * SCALE, qx - 2 * SCALE, qy - 2 * SCALE], fill=C_ON_PRIMARY)
    draw.rectangle([qx + 2 * SCALE, qy - 4 * SCALE, qx + 4 * SCALE, qy - 2 * SCALE], fill=C_ON_PRIMARY)
    draw.rectangle([qx - 4 * SCALE, qy + 2 * SCALE, qx - 2 * SCALE, qy + 4 * SCALE], fill=C_ON_PRIMARY)
    
    draw.text((qx + 18 * SCALE, qy), btn_lbl, fill=C_ON_PRIMARY, font=f_btn, anchor="lm")
    
    y += 66 * SCALE
    
    # Search input field with vector magnifier
    srch_txt = "Пошук серед 46 деталей на борту..." if lang == "uk" else "Search 46 spare parts aboard..."
    draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + 42 * SCALE],
                           radius=10 * SCALE, fill=C_SURFACE, outline=C_DIVIDER, width=max(1, round(1 * SCALE)))
    
    # Magnifier
    mx = 34 * SCALE
    my = y + 21 * SCALE
    r = 5 * SCALE
    draw.ellipse([mx - r, my - r, mx + r, my + r], outline=C_MUTED, width=2 * SCALE)
    draw.line([mx + 3 * SCALE, my + 3 * SCALE, mx + 8 * SCALE, my + 8 * SCALE], fill=C_MUTED, width=2 * SCALE)
    
    draw.text((mx + 16 * SCALE, my), srch_txt, fill=C_MUTED, font=get_font(12), anchor="lm")
    
    y += 56 * SCALE
    
    # Locker Group 1
    g1_title = "РУНДУК ЛІВОГО БОРТУ (САЛОН)" if lang == "uk" else "PORT SALON LOCKER"
    draw.text((18 * SCALE, y), g1_title, fill=C_MUTED, font=get_font(10, bold=True))
    y += 18 * SCALE
    
    items1 = [
        ("Паливний фільтр Racor 500FG", "Для паливного сепаратора · #2010PM-OR", "В наявності: 3 шт.", "В нормі (мін. 2)", False),
        ("Масляний фільтр Volvo 21718912", "Для двигуна Volvo D2-55", "В наявності: 1 шт.", "Мало! (мін. 2)", True),
    ] if lang == "uk" else [
        ("Fuel filter element Racor 500FG", "For fuel turbine separator · #2010PM-OR", "In stock: 3 pcs", "OK (min. 2)", False),
        ("Engine oil filter Volvo 21718912", "For Volvo D2-55 inboard diesel", "In stock: 1 pc", "Low stock (min. 2)", True),
    ]
    
    for name, spec, stock, status, is_alert in items1:
        card_h = 76 * SCALE
        draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + card_h],
                               radius=12 * SCALE, fill=C_SURFACE, outline=C_DIVIDER, width=max(1, round(1 * SCALE)))
        draw.text((28 * SCALE, y + 14 * SCALE), name, fill=C_INK, font=get_font(13, bold=True))
        draw.text((28 * SCALE, y + 34 * SCALE), spec, fill=C_MUTED, font=get_font(11))
        draw.text((28 * SCALE, y + 52 * SCALE), stock, fill=C_PRIMARY, font=get_font(11, semi=True))
        
        stat_color = C_OVERDUE if is_alert else C_OK
        draw.text((W - 28 * SCALE, y + 52 * SCALE), status, fill=stat_color, font=get_font(11, semi=True), anchor="ra")
        y += card_h + 10 * SCALE
        
    y += 10 * SCALE
    
    # Locker Group 2
    g2_title = "АХТЕРПІК / ТРЮМНИЙ ВІДСІК" if lang == "uk" else "LAZARETTE / AFT LOCKER"
    draw.text((18 * SCALE, y), g2_title, fill=C_MUTED, font=get_font(10, bold=True))
    y += 18 * SCALE
    
    items2 = [
        ("Крильчатка помпи Johnson 09-1027B", "Запасні імпелери зі змазкою", "В наявності: 2 шт.", "В нормі (мін. 1)", False),
        ("Ремінь генератора зубчастий 10x975", "Optibelt Marathon 1", "В наявності: 2 шт.", "В нормі (мін. 1)", False),
    ] if lang == "uk" else [
        ("Raw water impeller Johnson 09-1027B", "Spare neoprene impellers with lube", "In stock: 2 pcs", "OK (min. 1)", False),
        ("Alternator V-belt 10x975", "Optibelt Marathon 1", "In stock: 2 pcs", "OK (min. 1)", False),
    ]
    
    for name, spec, stock, status, is_alert in items2:
        card_h = 76 * SCALE
        draw.rounded_rectangle([16 * SCALE, y, W - 16 * SCALE, y + card_h],
                               radius=12 * SCALE, fill=C_SURFACE, outline=C_DIVIDER, width=max(1, round(1 * SCALE)))
        draw.text((28 * SCALE, y + 14 * SCALE), name, fill=C_INK, font=get_font(13, bold=True))
        draw.text((28 * SCALE, y + 34 * SCALE), spec, fill=C_MUTED, font=get_font(11))
        draw.text((28 * SCALE, y + 52 * SCALE), stock, fill=C_PRIMARY, font=get_font(11, semi=True))
        
        stat_color = C_OVERDUE if is_alert else C_OK
        draw.text((W - 28 * SCALE, y + 52 * SCALE), status, fill=stat_color, font=get_font(11, semi=True), anchor="ra")
        y += card_h + 10 * SCALE
        
    return img


# =========================================================================
# EXPORT ALL RESIZED
# =========================================================================
def save_scaled(img, filename):
    # Resize with high quality Lanczos downsampling
    scaled = img.resize((W // SCALE, H // SCALE), Image.Resampling.LANCZOS)
    target = os.path.join(OUT_DIR, filename)
    scaled.save(target, "PNG", optimize=True)
    print(f"Saved: {target} ({os.path.getsize(target)} bytes)")

def main():
    print("Generating screenshots...")
    # Ukrainian
    save_scaled(make_now_screen("uk", is_night=False), "now-uk.png")
    save_scaled(make_journal_screen("uk"), "journal-uk.png")
    save_scaled(make_store_screen("uk"), "store-uk.png")
    save_scaled(make_now_screen("uk", is_night=True), "night-uk.png")
    
    # English
    save_scaled(make_now_screen("en", is_night=False), "now-en.png")
    save_scaled(make_journal_screen("en"), "journal-en.png")
    save_scaled(make_store_screen("en"), "store-en.png")
    save_scaled(make_now_screen("en", is_night=True), "night-en.png")
    print("Done generating all 8 screenshots!")

if __name__ == "__main__":
    main()
