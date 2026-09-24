#!/usr/bin/env python3
"""Generate the canonical boat maintenance spreadsheet.

Two jobs at once:
  1. A genuinely useful free spreadsheet to hand out on owner forums, where
     people constantly ask each other for one ("send me a PM for my workbook").
  2. The reference layout our importer recognises with a 100% column mapping,
     via the fingerprint on the hidden _meta sheet.

Run:  python tools/make_template.py
Out:  template/Boat maintenance log.xlsx
"""
import json
import os

from openpyxl import Workbook
from openpyxl.formatting.rule import FormulaRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

OUT_DIR = "template"
OUT_FILE = os.path.join(OUT_DIR, "Boat maintenance log.xlsx")

FINGERPRINT = "HULLBEAT-TEMPLATE"
TEMPLATE_VERSION = 2
DATA_ROWS = 400

# Brand palette - see design/README.md
BRAND = "FF14403A"
BRAND_TINT = "FFE6F0ED"
INK = "FF0B1F1C"
MUTED = "FF4A5F5B"
OVERDUE_BG, OVERDUE_FG = "FFFBE4E2", "FFB3261E"
SOON_BG, SOON_FG = "FFFDF0DC", "FF8A5200"
OK_BG, OK_FG = "FFF2F8F6", "FF4A5F5B"
DEFERRED_BG, DEFERRED_FG = "FFE6F0ED", "FF14403A"

HEADER_FONT = Font(name="Calibri", size=11, bold=True, color="FFFFFFFF")
HEADER_FILL = PatternFill("solid", start_color=BRAND)
TITLE_FONT = Font(name="Calibri", size=16, bold=True, color=BRAND)
BODY_FONT = Font(name="Calibri", size=11, color=INK)
NOTE_FONT = Font(name="Calibri", size=10, color=MUTED)
THIN = Side(style="thin", color="FFD3E0DC")
CELL_BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)

# Categories are READ from the catalog, never restated here. They drifted once
# already ("Bilge & water" vs "Bilge & water systems") and a mismatch quietly
# breaks the importer, which matches the dropdown against catalog category names.
def _load_categories():
    with open(os.path.join("catalog", "catalog.json"), encoding="utf-8") as fh:
        return [c["en"] for c in json.load(fh)["categories"]]


CATEGORIES = _load_categories()
WORK_TYPES = [
    "Scheduled", "Repair", "Inspection", "Winterization", "Commissioning",
    "Haul-out", "Launch", "Upgrade", "Warranty", "Pre-sale",
]
PRECISIONS = ["Day", "Month", "Season", "Year"]
UNITS = ["hours", "nautical miles", "miles", "kilometres"]
YES_NO = ["Yes", "No"]
SOURCES = ["Manual", "Photo of meter", "From invoice"]

# sheet name -> (columns, [(width, note)]) ------------------------------------
SHEETS = {
    "Service log": [
        ("Date", 12, "ISO please: 2026-09-05"),
        ("Date precision", 14, "Day / Month / Season / Year - use it when backfilling from memory"),
        ("Category", 24, ""),
        ("Component", 28, "Impeller, anodes, standing rigging..."),
        ("What was done", 44, ""),
        ("Work type", 16, ""),
        ("Engine hours", 13, "Reading at the time of the job"),
        ("Miles", 10, ""),
        ("Cost", 10, ""),
        ("Currency", 10, ""),
        ("Parts used", 28, ""),
        ("Part numbers", 22, "The single most useful thing you will ever write down"),
        ("Done by", 18, "You, or the yard"),
        ("Notes", 40, ""),
        ("Photo / receipt", 26, "File name or link"),
    ],
    "Hours & miles": [
        ("Date", 12, ""),
        ("Meter", 18, "Engine 1, Engine 2, Log..."),
        ("Reading", 12, ""),
        ("Unit", 16, ""),
        ("Source", 18, ""),
        ("Note", 36, ""),
    ],
    "Schedule": [
        ("Category", 24, ""),
        ("Component", 28, ""),
        ("Every (days)", 13, ""),
        ("Every (hours)", 14, ""),
        ("Every (miles)", 13, ""),
        ("Last done", 12, ""),
        ("Expiry date", 12, "Printed on the item - overrides the interval"),
        ("Defer until", 12, "Knowingly putting it off? Say until when - it stops nagging"),
        ("Why deferred", 26, "Waiting for the yard, for a part, for the haul-out..."),
        ("Next due", 12, "Calculated"),
        ("Status", 13, "Calculated"),
        ("Checked against manual", 20, "Engine intervals vary - confirm yours"),
        ("Notes", 34, ""),
    ],
    "Spec sheet": [
        ("Category", 24, ""),
        ("Component", 28, ""),
        ("Spec", 26, "Oil grade, capacity, filter part no, torque..."),
        ("Value", 30, ""),
        ("Notes", 34, ""),
    ],
    "Inventory": [
        ("Part", 30, ""),
        ("Part number", 22, ""),
        ("Manufacturer", 20, ""),
        ("Quantity", 10, ""),
        ("Minimum", 10, "Below this it goes on the shopping list"),
        ("Location", 24, "Locker 3, port lazarette, under nav seat..."),
        ("For component", 26, ""),
        ("Notes", 30, ""),
    ],
    "Documents": [
        ("Document", 30, "Registration, insurance, radio licence, inspection..."),
        ("Reference", 22, ""),
        ("Issued", 12, ""),
        ("Expires", 12, ""),
        ("Notice needed (days)", 18, "How long the renewal really takes. Passport 240, insurance 30"),
        ("Next due", 12, "Calculated"),
        ("Status", 13, "Calculated"),
        ("Notes", 34, ""),
    ],
}

INTRO = [
    ("Boat maintenance log", "title"),
    ("", None),
    ("A free spreadsheet for keeping your boat's service history. No sign-up, no strings.", "body"),
    ("", None),
    ("How to use it", "h2"),
    ("1.  Fill in Service log every time you do something. One row per job.", "body"),
    ("2.  Put a line in Hours & miles whenever you glance at the hour meter.", "body"),
    ("    The more of those you have, the better anything can predict what is coming.", "note"),
    ("3.  Set your intervals once in Schedule. The Status column then turns amber", "body"),
    ("    30 days out and red when something is overdue.", "note"),
    ("4.  Spec sheet is the one that saves you in the chandlery: oil grade, capacities,", "body"),
    ("    filter part numbers. Fill it in once, thank yourself for years.", "note"),
    ("", None),
    ("Dates", "h2"),
    ("Use 2026-09-05 format. It sorts correctly and no one has to guess whether", "body"),
    ("03/04 means March or April.", "note"),
    ("Backfilling old work from memory? Put Month, Season or Year in Date precision", "body"),
    ("rather than inventing a day - a rough date you have marked as rough is honest data.", "note"),
    ("", None),
    ("Add your own rows, columns and sheets", "h2"),
    ("Every boat is different and no template survives contact with a real one.", "body"),
    ("Nothing here breaks if you change it.", "note"),
    ("", None),
    ("Keep a copy somewhere else", "h2"),
    ("Boats outlive apps and phones. Whatever you use, keep a copy of this file", "body"),
    ("in your own cloud drive. That way the record is yours no matter what.", "note"),
]


def style_header(ws, columns, row=1):
    for index, (name, width, note) in enumerate(columns, start=1):
        cell = ws.cell(row=row, column=index, value=name)
        cell.font = HEADER_FONT
        cell.fill = HEADER_FILL
        cell.alignment = Alignment(vertical="center", wrap_text=True)
        letter = get_column_letter(index)
        ws.column_dimensions[letter].width = width
        if note:
            cell.comment = None  # comments bloat the file; the hint row carries it
    ws.row_dimensions[row].height = 26
    # Hint row in muted text, so the meaning of a column is never a mystery.
    for index, (_, _, note) in enumerate(columns, start=1):
        hint = ws.cell(row=row + 1, column=index, value=note or "")
        hint.font = NOTE_FONT
        hint.alignment = Alignment(vertical="top", wrap_text=True)
    ws.row_dimensions[row + 1].height = 24
    ws.freeze_panes = ws.cell(row=row + 2, column=1)


def write_lists(meta):
    """Park the lookup lists on the hidden sheet and return range references.

    Inline lists are tempting but Excel caps a data-validation formula at 255
    characters; our 21 categories come to 313 and Excel then declares the whole
    file corrupt and silently drops the dropdown. Ranges have no such limit.
    """
    lists = {
        "Categories": (CATEGORIES, "D"),
        "WorkTypes": (WORK_TYPES, "E"),
        "Precisions": (PRECISIONS, "F"),
        "Units": (UNITS, "G"),
        "YesNo": (YES_NO, "H"),
        "Sources": (SOURCES, "I"),
    }
    refs = {}
    for title, (values, column) in lists.items():
        meta[f"{column}1"] = title
        meta[f"{column}1"].font = Font(bold=True, color=MUTED)
        for offset, value in enumerate(values, start=2):
            meta[f"{column}{offset}"] = value
        meta.column_dimensions[column].width = 26
        refs[title] = f"'{meta.title}'!${column}$2:${column}${len(values) + 1}"
    return refs


def add_validation(ws, column_letter, source_ref, first=3, last=DATA_ROWS):
    dv = DataValidation(
        type="list", formula1=source_ref, allow_blank=True, showErrorMessage=False
    )
    ws.add_data_validation(dv)
    dv.add(f"{column_letter}{first}:{column_letter}{last}")


def date_format(ws, column_letter, first=3, last=DATA_ROWS):
    for row in range(first, last + 1):
        ws[f"{column_letter}{row}"].number_format = "yyyy-mm-dd"


def traffic_light(ws, status_range):
    """The red/amber/neutral scale from design/README.md.

    Deliberately not red/amber/green: green reads as 'brand', and red/green is
    the hardest pair for the ~6% of men with deuteranopia - a real slice of this
    audience. The words carry the meaning; colour only reinforces it.
    """
    rules = [
        ('OVERDUE', OVERDUE_BG, OVERDUE_FG),
        ('DUE SOON', SOON_BG, SOON_FG),
        ('OK', OK_BG, OK_FG),
        # The one place brand colour carries meaning: DEFERRED is not a health
        # status, it is "you have decided about this". Borrowed from MV Dirona,
        # where an acknowledged item sits green while 37 months overdue.
        ('DEFERRED', DEFERRED_BG, DEFERRED_FG),
    ]
    for text, bg, fg in rules:
        first_cell = status_range.split(":")[0]
        ws.conditional_formatting.add(
            status_range,
            FormulaRule(
                formula=[f'EXACT(${first_cell[0]}{first_cell[1:]},"{text}")'.replace(
                    f'${first_cell[0]}{first_cell[1:]}', f'{first_cell}')],
                stopIfTrue=False,
                fill=PatternFill("solid", start_color=bg),
                font=Font(color=fg, bold=(text != "OK")),
            ),
        )


def build():
    wb = Workbook()

    # ---------------------------------------------------------------- intro
    intro = wb.active
    intro.title = "Start here"
    intro.sheet_view.showGridLines = False
    intro.column_dimensions["A"].width = 100
    row = 2
    for text, kind in INTRO:
        cell = intro.cell(row=row, column=1, value=text)
        if kind == "title":
            cell.font = TITLE_FONT
        elif kind == "h2":
            cell.font = Font(name="Calibri", size=12, bold=True, color=BRAND)
        elif kind == "note":
            cell.font = NOTE_FONT
        elif kind == "body":
            cell.font = BODY_FONT
        row += 1

    # ---------------------------------------------------------------- meta
    # Built first: the dropdowns below reference ranges on this sheet.
    # Hidden fingerprint - the importer reads it and maps every column with no
    # questions asked. Without it the file still imports, just with the usual
    # column-mapping step.
    meta = wb.create_sheet("_meta")
    meta["A1"] = "fingerprint"
    meta["B1"] = FINGERPRINT
    meta["A2"] = "template_version"
    meta["B2"] = TEMPLATE_VERSION
    meta["A3"] = "note"
    meta["B3"] = "Do not delete this sheet if you want one-tap import."
    meta.column_dimensions["A"].width = 18
    meta.column_dimensions["B"].width = 60
    refs = write_lists(meta)

    # -------------------------------------------------------------- sheets
    for name, columns in SHEETS.items():
        ws = wb.create_sheet(name)
        ws.sheet_view.showGridLines = False
        style_header(ws, columns)
        for r in range(3, DATA_ROWS + 1):
            for c in range(1, len(columns) + 1):
                ws.cell(row=r, column=c).border = CELL_BORDER

    # Service log
    log = wb["Service log"]
    date_format(log, "A")
    add_validation(log, "B", refs["Precisions"])
    add_validation(log, "C", refs["Categories"])
    add_validation(log, "F", refs["WorkTypes"])

    # Hours & miles
    hours = wb["Hours & miles"]
    date_format(hours, "A")
    add_validation(hours, "D", refs["Units"])
    add_validation(hours, "E", refs["Sources"])

    # Schedule: the traffic light owners already build by hand in Excel.
    sched = wb["Schedule"]
    for column in ("F", "G", "H", "J"):
        date_format(sched, column)
    add_validation(sched, "A", refs["Categories"])
    add_validation(sched, "L", refs["YesNo"])
    for r in range(3, DATA_ROWS + 1):
        # A printed expiry beats a computed interval - same rule as the app.
        sched[f"J{r}"] = (
            f'=IF(G{r}<>"",G{r},IF(AND(C{r}<>"",F{r}<>""),F{r}+C{r},""))'
        )
        # A live deferral outranks the date: the owner already knows and decided.
        sched[f"K{r}"] = (
            f'=IF(AND(H{r}<>"",H{r}>=TODAY()),"DEFERRED",'
            f'IF(J{r}="","",IF(J{r}<TODAY(),"OVERDUE",'
            f'IF(J{r}<=TODAY()+30,"DUE SOON","OK"))))'
        )
        sched[f"J{r}"].number_format = "yyyy-mm-dd"
        sched[f"K{r}"].alignment = Alignment(horizontal="center")
    traffic_light(sched, f"K3:K{DATA_ROWS}")

    # Spec sheet. Inventory column A is a free-text part name - no dropdown.
    add_validation(wb["Spec sheet"], "A", refs["Categories"])

    # Documents
    docs = wb["Documents"]
    for column in ("C", "D", "F"):
        date_format(docs, column)
    for r in range(3, DATA_ROWS + 1):
        docs[f"F{r}"] = f'=IF(D{r}<>"",D{r},"")'
        # Notice period is per document, not a single rule for everything:
        # a passport needs months of warning, a prepaid SIM needs weeks.
        docs[f"G{r}"] = (
            f'=IF(F{r}="","",IF(F{r}<TODAY(),"OVERDUE",'
            f'IF(F{r}<=TODAY()+IF(E{r}<>"",E{r},30),"DUE SOON","OK")))'
        )
        docs[f"F{r}"].number_format = "yyyy-mm-dd"
        docs[f"G{r}"].alignment = Alignment(horizontal="center")
    traffic_light(docs, f"G3:G{DATA_ROWS}")

    meta.sheet_state = "hidden"

    os.makedirs(OUT_DIR, exist_ok=True)
    wb.save(OUT_FILE)
    return OUT_FILE


if __name__ == "__main__":
    path = build()
    size = os.path.getsize(path)
    print(f"written {path}  ({size / 1024:.1f} KB)")
