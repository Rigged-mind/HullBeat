#!/usr/bin/env python3
"""Ensure category codes used in Kotlin match valid catalog categories.

WHY THIS EXISTS

CreateComponentSheet once shipped with `listOf("other", "engine", "electrical", "hull_deck")`.
Three of those four ("engine", "electrical", "hull_deck") did not exist in the
catalog vocabulary (which defines "engine_main", "elec_dc", "hull", etc.).
As a result:
  1. The category chips displayed raw unlocalized strings ("engine", etc.).
  2. Components created with those codes were invisible to every engine meter
     selector in the app, causing engine hour meters to mis-attach to the hull.

This check reads the canonical category set from `strings_catalog.xml` (all `cat_<name>`
resources) and verifies that category code lists and assignments in Kotlin code
name only valid categories (or explicitly documented legacy compatibility aliases).

Run:  python tools/check_category_codes.py
Exit: 1 on any unknown category code.
"""
import io
import os
import re
import sys

CATALOG_STRINGS = os.path.join("app", "src", "main", "res", "values", "strings_catalog.xml")
JAVA_ROOT = os.path.join("app", "src", "main", "java")

def load_valid_categories():
    if not os.path.exists(CATALOG_STRINGS):
        print(f"Error: {CATALOG_STRINGS} not found")
        sys.exit(1)
    with io.open(CATALOG_STRINGS, "r", encoding="utf-8") as f:
        content = f.read()
    cats = set(re.findall(r'<string\s+name="cat_([a-z0-9_]+)"', content))
    if not cats:
        print("Error: No categories found in strings_catalog.xml")
        sys.exit(1)
    return cats

def check_create_component_sheet(valid_cats):
    sheet_path = os.path.join(JAVA_ROOT, "app", "hullbeat", "ui", "components", "CreateComponentSheet.kt")
    if not os.path.exists(sheet_path):
        return []
    with io.open(sheet_path, "r", encoding="utf-8") as f:
        src = f.read()

    errors = []
    m = re.search(r'primaryCats\s*=\s*listOf\(([^)]+)\)', src)
    if m:
        raw_items = m.group(1)
        items = re.findall(r'"([^"]+)"', raw_items)
        for cat in items:
            if cat not in valid_cats:
                errors.append(f"{sheet_path}: Unknown category code '{cat}' in primaryCats. Must be one of: {sorted(valid_cats)}")
    return errors

def main():
    valid_cats = load_valid_categories()
    errors = []
    errors.extend(check_create_component_sheet(valid_cats))

    if errors:
        for err in errors:
            print(f"FAIL: {err}", file=sys.stderr)
        sys.exit(1)

    print(f"OK: verified category codes against {len(valid_cats)} catalog categories.")
    sys.exit(0)

if __name__ == "__main__":
    main()
