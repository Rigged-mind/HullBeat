#!/usr/bin/env python3
"""Run every generator and check, in order, and report what moved.

The eight tools existed. Nothing made them run. Through this whole session I
invoked them by hand in a shell loop, which is the last instance of the defect
that dominated the day: a rule that is correct and not carried to its last
point of use. A validator nobody runs is a comment.

Two things this does that running them separately does not:

  * ORDER. reference_vessels reads checklists.json, so it has to come after
    make_checklists; validate_plurals reads the prototype, so it comes after
    make_prototype_data. Run them alphabetically and a stale input passes.

  * DRIFT. Every generated file is hashed before and after. If a generator
    rewrites something, the committed copy was out of date - somebody edited a
    derived file by hand, or changed a source and never regenerated. That is
    exactly how "джекштоки" survived in the checklists while the catalog
    already said otherwise, and it is invisible when each tool exits 0.

Run:  python tools/check_all.py
      python tools/check_all.py --check   (fail if any generated file changes)
Exit: 1 on any failure, or on drift when --check is given.
"""
import hashlib
import io
import os
import subprocess
import sys

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Order is the point: sources first, then whatever reads their output.
# check_prototype is here because 8/8 once passed on a prototype whose
# script did not parse: a duplicate `const` killed every screen and not
# one step executed a line of JavaScript.
# Descriptions carry no counts on purpose: a number typed here cannot be
# checked by anything, and two of them had already drifted - this table
# claimed 4 reference boats out of 5 and 152 passport labels out of 154.
STEPS = [
    ("make_palette", "Color.kt, colors.xml and the CSS from one palette"),
    ("make_search_config", "SearchConfig.kt from design/search-ranking.json"),
    ("check_contrast", "every scheme against WCAG, all four of them"),
    ("validate_catalog", "catalog structure, vocabulary, synonym duplicates"),
    ("make_strings", "Android string resources + synonyms.json"),
    ("make_checklists", "checklist items, node links, mood, expiry"),
    ("make_spechints", "passport labels for the component pages"),
    ("make_prototype_data", "the prototype's copy of catalog + checklists"),
    ("check_prototype", "the prototype parses, and its tokens exist"),
    ("check_entities", "every @Entity is registered with Room"),
    ("check_manifest", "every class the manifest names exists"),
    ("check_kotlin_syntax", "every Kotlin file lexes: strings, brackets"),
    ("check_kotlin_imports", "every type a Kotlin file names is visible"),
    ("check_dao_methods", "every db.xxxDao().method() actually exists"),
    ("check_room_sql", "every @Query prepared by a real SQLite engine"),
    ("check_migrations", "Room 1->2 migration alters and preserves data"),
    ("check_enum_constants", "every Enum.CONSTANT reference exists"),
    ("check_category_codes", "every category code used in Kotlin exists in catalog"),
    ("check_hardcoded_strings", "no interface text is typed into Kotlin"),
    ("check_resource_keep", "a name-resolved resource survives shrinking"),
    ("check_resource_names", "one name, one resource - what aapt2 refuses"),
    ("check_resource_escapes", "apostrophes, quotes and %s - what aapt2 rejects"),
    ("check_touch_targets", "nothing you tap is smaller than 56 dp"),
    ("check_canvas_lifetime", "no local holds a canvas a page break frees"),
    ("check_translations", "no string falls back to English silently"),
    ("prototype_due", "DueCalculator cases, JS against the Kotlin"),
    ("check_search", "search ranking, SQLite FTS4 and benchmark queries"),
    ("check_node_gates", "no checklist item outlives the node it names"),
    ("reference_vessels", "reference boats: what must be true of each"),
    ("validate_plurals", "quantity strings, and JS vs XML agreement"),
]

# Everything a generator writes. Hashing these is how drift becomes visible.
GENERATED = [
    os.path.join("catalog", "checklists.json"),
    os.path.join("catalog", "synonyms.json"),
    os.path.join("catalog", "i18n", "uk.json"),
    os.path.join("catalog", "i18n", "uk-review.md"),
    os.path.join("catalog", "i18n", "uk-checklists-review.md"),
    os.path.join("catalog", "i18n", "uk-spechints-review.md"),
    os.path.join("prototype", "index.html"),
    os.path.join("app", "src", "main", "java", "app", "hullbeat", "ui",
                 "theme", "Color.kt"),
    os.path.join("app", "src", "main", "java", "app", "hullbeat", "domain",
                 "search", "SearchConfig.kt"),
    os.path.join("app", "src", "main", "res", "values", "colors.xml"),
    os.path.join("app", "src", "main", "res", "values-night",
                 "colors.xml"),
    os.path.join("app", "src", "main", "res", "values", "strings_catalog.xml"),
    os.path.join("app", "src", "main", "res", "values-uk", "strings_catalog.xml"),
]


def digest(path):
    if not os.path.exists(path):
        return None
    return hashlib.sha256(io.open(path, "rb").read()).hexdigest()

# Gradle's copyCatalog writes these into assets at preBuild. A copy that sits
# here and DISAGREES with the source is either hand-edited or left over from an
# older version, and it will answer a grep with a rule the app no longer uses.
ASSET_COPIES = [
    (os.path.join("app", "src", "main", "assets", "catalog", name),
     os.path.join("catalog", name))
    for name in ("catalog.json", "synonyms.json", "checklists.json")
]


def check_asset_copies():
    """Report any assets copy that differs from the file it is copied from."""
    stale = []
    for copy, src in ASSET_COPIES:
        if os.path.exists(copy) and digest(copy) != digest(src):
            stale.append(copy)
    return stale



def main(argv):
    strict = "--check" in argv
    before = {p: digest(p) for p in GENERATED}

    failures = []
    print(f"{'step':22} {'':4} what it guards")
    print("-" * 78)
    for name, what in STEPS:
        proc = subprocess.run(
            [sys.executable, os.path.join("tools", f"{name}.py")],
            capture_output=True, text=True, encoding="utf-8", errors="replace")
        ok = proc.returncode == 0
        print(f"{name:22} {'OK' if ok else 'FAIL':4} {what}")
        if not ok:
            failures.append(name)
            for line in (proc.stdout + proc.stderr).splitlines():
                if line.strip():
                    print(f"    | {line.rstrip()}")

    moved = [p for p in GENERATED if digest(p) != before[p]]
    stale_assets = check_asset_copies()
    if stale_assets:
        print()
        print("  stale copy under assets - Gradle overwrites it at build, but "
              "it disagrees with /catalog now:")
        for path in stale_assets:
            print("    " + path)
        failures.append("stale asset copy")
    print()
    if moved:
        print(f"{len(moved)} generated file(s) changed - the committed copies "
              "were stale:")
        for p in moved:
            print(f"    {p}")
        if strict:
            failures.append("drift")
    else:
        print("no generated file changed: sources and outputs agree")

    if failures:
        print(f"\nFAILED: {', '.join(failures)}")
        return 1
    print(f"\nall {len(STEPS)} steps pass")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
