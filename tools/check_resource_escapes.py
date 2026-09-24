#!/usr/bin/env python3
"""What aapt2 refuses inside a string resource, before aapt2 sees it.

    <string name="pdf_footer_brand">HullBeat Ship's Log</string>

built fine as a Kotlin default for months. The moment it became a string
resource the build died with

    Can not extract resource from com.android.aaptcompiler.ParsedResource@...

which names neither the file, nor the line, nor the apostrophe. All 28 other
steps were green: `check_translations` compares name sets, `check_resource_names`
counts duplicates, and neither of them looks at what is between the tags.

Four rules, all of them aapt2 errors rather than style:

  '     an apostrophe must be escaped, or the whole value quoted
  "     quotes come in pairs; a lone one opens a span that never closes
  @ ?   a value starting with either is read as a resource reference
  %     two or more substitutions must be positional - %1$s, not %s

Entities are resolved first, exactly as aapt2 resolves them, so `&apos;`
is caught as surely as a typed apostrophe.
"""

import glob
import io
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

SUBSTITUTION = re.compile(r"%(?!%)(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z]")


def problems_in(value):
    """Everything aapt2 would reject in one string value."""
    found = []

    quoted = False
    i = 0
    while i < len(value):
        ch = value[i]
        if ch == "\\":
            i += 2
            continue
        if ch == '"':
            quoted = not quoted
        elif ch == "'" and not quoted:
            found.append("an unescaped apostrophe - write \\' or wrap the "
                         "whole value in \"")
        i += 1
    if quoted:
        found.append('an unpaired " - it opens a quoted span that never closes')

    stripped = value.lstrip()
    if stripped[:1] in ("@", "?") and not stripped.startswith(("@\\", "?\\")):
        found.append("starts with %r, which aapt2 reads as a resource "
                     "reference - escape it" % stripped[0])

    subs = SUBSTITUTION.findall(value)
    if len(subs) > 1 and any(not positional for positional, _ in subs):
        found.append("%d substitutions but not all are positional - use "
                     "%%1$s, %%2$s..." % len(subs))

    return found


def text_of(element):
    """The value as aapt2 sees it: entities resolved, inline tags flattened."""
    return "".join(element.itertext())


def main():
    failures = 0
    checked = 0
    for path in sorted(glob.glob(os.path.join(RES, "values*", "*.xml"))):
        rel = os.path.relpath(path, ROOT).replace("\\", "/")
        raw = io.open(path, encoding="utf-8").read()
        try:
            root = ET.fromstring(raw)
        except ET.ParseError as exc:
            print("    %s: not well-formed XML - %s" % (rel, exc))
            failures += 1
            continue

        # Only text resources. An `<item>` inside a `<style>` is a reference
        # by design - the first version of this checker flagged three of them
        # in themes.xml and would have taught everyone to ignore it.
        values = []
        for element in root:
            if element.tag == "string":
                values.append((element.get("name"), element))
            elif element.tag in ("plurals", "string-array"):
                for item in element.findall("item"):
                    label = "%s[%s]" % (element.get("name"),
                                        item.get("quantity") or "item")
                    values.append((label, item))

        for name, element in values:
            value = text_of(element)
            if not value:
                continue
            checked += 1
            for problem in problems_in(value):
                # Line numbers: ElementTree does not keep them, so find the
                # value in the raw text. Exact enough to click on.
                line = raw.count("\n", 0, raw.find(value)) + 1 if value in raw else 0
                print("    %s:%d  %s: %s" % (rel, line, name or "item", problem))
                failures += 1

    print("%-22s %s" % ("resource escaping",
                        "%d values, %d aapt2 would reject" % (checked, failures)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
