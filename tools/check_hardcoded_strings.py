#!/usr/bin/env python3
"""No user-facing text may be written into Kotlin as a literal.

WHY THIS EXISTS

`check_translations.py` compares `values/` against `values-uk/`, so it proves
every resource is translated. It says nothing about text that never became a
resource. A Ukrainian sentence typed straight into a Composable passes all
sixteen checks, ships, and shows Ukrainian to an English-locale owner - and
the app cannot be translated into a third language at all, because half its
words are in the source.

That is exactly what happened to the first Now screen: seven literals -
"Запис збережено", "Скасувати", three tab descriptions - written in Kotlin
where every other string in the project lives in resources.

The rule is one-directional and easy to state: **Cyrillic in a Kotlin string
literal is user-facing text in the wrong place.** Comments and KDoc are
stripped first, because explaining a marine term in Ukrainian inside a comment
is the right thing to do and has nothing to do with this.

ALLOWED is for the rare literal that is genuinely data, not text - a value
stored in the database, a fixture in a test. Each entry needs a reason;
"it was easier" is not one.

Run:  python tools/check_hardcoded_strings.py
Exit: 1 if any Kotlin file carries user-facing text.
"""
import io
import os
import re
import sys

ROOTS = [os.path.join("app", "src", "main", "java")]

# Literals that are data rather than interface text. Empty on purpose: the
# moment this list has entries, each one wants a comment saying why.
ALLOWED = {
    # Historic database strings stored in SQLite prior to dynamic re-localization:
    "Зроблено",
    "Виконано зі сповіщення",
    "Продовжено",
    "Продовжити…",
    "Продовжити...",
    "Моє судно",
    "Моторний відсік",
    "Рундук кокпіта",
    "Штурманський стіл",
    "Форпік",
    "Камбуз",
}

CYRILLIC = re.compile(r"[Ѐ-ӿ]")
LITERAL = re.compile(r'"((?:[^"\\\n]|\\.)*)"')


def strip_noise(src):
    """Comments and KDoc go first: Ukrainian in a comment is not a defect."""
    src = re.sub(r"/\*(?:.|\n)*?\*/", "", src)
    src = re.sub(r"//[^\n]*", "", src)
    return src


def main():
    files = []
    for root in ROOTS:
        for dirpath, _, names in os.walk(root):
            files += [os.path.join(dirpath, n) for n in names if n.endswith(".kt")]

    problems = []
    scanned = 0
    for path in sorted(files):
        raw = io.open(path, encoding="utf-8").read()
        body = strip_noise(raw)
        for m in LITERAL.finditer(body):
            text = m.group(1)
            scanned += 1
            if not CYRILLIC.search(text) or text in ALLOWED:
                continue
            # Line number from the stripped body is close enough to be useful
            # and honest about being approximate.
            line = body[: m.start()].count("\n") + 1
            problems.append(
                '%s:~%d: "%s" is interface text in Kotlin - it belongs in '
                "values/strings.xml with a values-uk translation, or the app "
                "shows Ukrainian to an English owner"
                % (path, line, text if len(text) <= 60 else text[:57] + "..."))

    for p in problems:
        print("  ERROR " + p)
    print("%d Kotlin files, %d string literals scanned" % (len(files), scanned))
    if problems:
        print("FAILED: %d hardcoded string(s)" % len(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
