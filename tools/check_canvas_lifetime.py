#!/usr/bin/env python3
"""No Kotlin local may hold a Canvas it does not own.

The passport export killed the app - not with an exception, with SIGSEGV,
fault addr 0x0, inside libhwui's Canvas::drawText. Three tombstones, all the
same, all in drawEquipmentSection.

Every section opened like this:

    private fun drawEquipmentSection(pdf: PdfContext, ...) {
        val c = pdf.canvas ?: return      // the canvas of page 1
        ...
        for (comp in components) {
            pdf.ensureSpace(16f)          // -> newPage -> document.finishPage
            c.drawText(comp.name, ...)    // page 1's canvas is gone
        }

`PdfDocument.finishPage` releases the page's native canvas. The Kotlin
reference survives, so nothing in the type system objects; the first draw
after a page break dereferences a null native pointer and the kernel kills
the process. No try/catch in the language can see that, which is why the
export "just closed the app" and left no message and no file.

A vessel seeded from the full catalogue has about forty components and a
page holds about twenty-nine rows, so it crashed every single time.

The rule this enforces is small and absolute: a Canvas you got from
somewhere else is read at the point of use, never stored. A Canvas you
constructed yourself (`Canvas(bitmap)`) is yours and is not flagged.

Worth keeping because the passport is about to grow a documents section,
and the shape that crashes is the shape you write by reflex.
"""

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app", "src", "main", "java")

# `val c = pdf.canvas`, `val c = page.canvas ?: return`, `var x = this.canvas`
BORROWED = re.compile(
    r"^\s*va[lr]\s+(\w+)\s*(?::\s*Canvas\??\s*)?=\s*"
    r"([\w.]*\bcanvas)\b(?!\s*=)",
    re.M,
)

# Calls that can end the page under you. `ensureSpace` is the dangerous one
# because it reads as a measurement, not as a mutation.
INVALIDATES = ("newPage(", "ensureSpace(", "finishPage(", "startPage(")


def strip_comments(text):
    """Blank comments out newline for newline, so line numbers stay exact."""
    out = []
    i, n = 0, len(text)
    while i < n:
        two = text[i:i + 2]
        if two == "//":
            j = text.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif two == "/*":
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:j]))
            i = j
        elif text[i] == '"':
            if text[i:i + 3] == '"""':
                j = text.find('"""', i + 3)
                j = n if j < 0 else j + 3
            else:
                j = i + 1
                while j < n and text[j] != '"':
                    j += 2 if text[j] == "\\" else 1
                j = min(j + 1, n)
            out.append("".join(ch if ch == "\n" else " " for ch in text[i:j]))
            i = j
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


def block_after(text, start):
    """The rest of the enclosing block, up to the brace that closes it."""
    depth = 0
    for i in range(start, len(text)):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            if depth == 0:
                return text[start:i]
            depth -= 1
    return text[start:]


def kotlin_files():
    for base, _, names in os.walk(SRC):
        for name in sorted(names):
            if name.endswith(".kt"):
                yield os.path.join(base, name)


def main():
    failures = []
    scanned = 0
    for path in kotlin_files():
        raw = io.open(path, encoding="utf-8").read()
        if "canvas" not in raw.lower():
            continue
        scanned += 1
        text = strip_comments(raw)
        rel = os.path.relpath(path, ROOT).replace("\\", "/")

        for match in BORROWED.finditer(text):
            name, source = match.group(1), match.group(2)
            line = text.count("\n", 0, match.start()) + 1

            # Everything the local can still reach: to the end of the block
            # that declares it, found by counting braces rather than by
            # looking for the next `fun`. A nested function is indented
            # differently from a top-level one, and the first version of this
            # checker read `drawPageDecorations` as running to the end of the
            # class - which made a safe local look like the crashing one.
            scope = block_after(text, match.end())

            used = re.search(r"(?<![\w.])%s\s*\.\s*draw" % re.escape(name), scope)
            risky = next((k for k in INVALIDATES if k in scope), None)
            if used and risky:
                failures.append((rel, line))
                print("    %s:%d  '%s' holds %s across %s - read it at the "
                      "point of use instead" % (rel, line, name, source, risky))
            elif used:
                failures.append((rel, line))
                print("    %s:%d  '%s' stores a borrowed canvas; the page can "
                      "be finished under it" % (rel, line, name))

    print("%-22s %s" % ("canvas lifetime",
                        "%d files drawing, %d holding a stale canvas"
                        % (scanned, len(failures))))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
