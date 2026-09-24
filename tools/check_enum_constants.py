#!/usr/bin/env python3
"""Every Enum.CONSTANT reference must name a constant the enum declares.

WHY THIS EXISTS

`check_kotlin_imports.py` skips ALL_CAPS identifiers on purpose - treating
every constant as a type to resolve would drown the output. That leaves a gap
exactly where a plausible-looking guess lives: `Criticality.MEDIUM` reads
perfectly and does not exist, because the enum declares HIGH, MED, LOW. The
first Now screen shipped with it, and all seventeen checks were green.

An enum constant that does not exist is a compile error, not a subtle bug -
which is the point. This check exists to move that error from the first build
back to the second before it, where it costs seconds instead of a round trip
through Android Studio.

Run:  python tools/check_enum_constants.py
Exit: 1 on any reference to a constant that is not declared.
"""
import io
import os
import re
import sys

ROOTS = [os.path.join("app", "src", "main", "java"),
         os.path.join("app", "src", "test", "java")]

ENUM_RE = re.compile(r"enum class (\w+)\s*(?:\([^)]*\))?\s*\{(.*?)\}", re.S)
CONST_RE = re.compile(r"\b([A-Z][A-Z0-9_]*)\b")


# Framework objects whose members are a closed, stable set. Anything not
# listed here is not judged at all.
FRAMEWORK = {
    # androidx.compose.ui.Alignment - the whole interface, both axes.
    "Alignment": {
        "TopStart", "TopCenter", "TopEnd",
        "CenterStart", "Center", "CenterEnd",
        "BottomStart", "BottomCenter", "BottomEnd",
        "Top", "CenterVertically", "Bottom",
        "Start", "CenterHorizontally", "End",
        "Vertical", "Horizontal",
    },
    # androidx.compose.foundation.layout.Arrangement
    "Arrangement": {
        "Start", "End", "Top", "Bottom", "Center",
        "SpaceBetween", "SpaceAround", "SpaceEvenly",
        "Absolute", "Vertical", "Horizontal", "HorizontalOrVertical",
    },
    # androidx.compose.ui.text.font.FontWeight
    "FontWeight": {
        "Thin", "ExtraLight", "Light", "Normal", "Medium",
        "SemiBold", "Bold", "ExtraBold", "Black",
        "W100", "W200", "W300", "W400", "W500",
        "W600", "W700", "W800", "W900",
    },
    # androidx.compose.ui.graphics.Color - the named ones only.
    "Color": {"Black", "DarkGray", "Gray", "LightGray", "White", "Red",
              "Green", "Blue", "Yellow", "Cyan", "Magenta", "Transparent",
              "Unspecified"},
    # kotlinx.coroutines.Dispatchers
    "Dispatchers": {"Default", "Main", "IO", "Unconfined"},
    # androidx.room
    "OnConflictStrategy": {"REPLACE", "ABORT", "FAIL", "IGNORE", "NONE"},
    "ForeignKey": {"NO_ACTION", "RESTRICT", "SET_NULL", "SET_DEFAULT",
                   "CASCADE"},
    # androidx.work
    "ExistingPeriodicWorkPolicy": {"KEEP", "REPLACE", "UPDATE",
                                   "CANCEL_AND_REENQUEUE"},
    "ExistingWorkPolicy": {"REPLACE", "KEEP", "APPEND", "APPEND_OR_REPLACE"},
    # java.util.concurrent.TimeUnit / java.time.temporal.ChronoUnit
    "TimeUnit": {"NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS",
                 "MINUTES", "HOURS", "DAYS"},
    "ChronoUnit": {"NANOS", "MICROS", "MILLIS", "SECONDS", "MINUTES",
                   "HOURS", "HALF_DAYS", "DAYS", "WEEKS", "MONTHS", "YEARS",
                   "DECADES", "CENTURIES", "MILLENNIA", "ERAS", "FOREVER"},
}


def strip_noise(src):
    src = re.sub(r"/\*(?:.|\n)*?\*/", "", src)
    return re.sub(r"//[^\n]*", "", src)


def main():
    files = []
    for root in ROOTS:
        for dirpath, _, names in os.walk(root):
            files += [os.path.join(dirpath, n) for n in names if n.endswith(".kt")]

    sources = {p: io.open(p, encoding="utf-8").read() for p in sorted(files)}

    enums = {}
    for src in sources.values():
        for m in ENUM_RE.finditer(src):
            # Constants come before the first `;`, which is where an enum's
            # own members would start.
            enums[m.group(1)] = set(CONST_RE.findall(m.group(2).split(";")[0]))
    if not enums:
        print("no enums found")
        return 1

    # Our enums use ALL_CAPS constants; the framework's members are
    # CamelCase, so both patterns have to be recognised.
    ours = re.compile(r"\b(" + "|".join(sorted(enums)) + r")\.([A-Z][A-Z0-9_]*)\b")
    theirs = re.compile(r"(?<![\w.])(" + "|".join(sorted(FRAMEWORK))
                        + r")\.([A-Z][A-Za-z0-9_]*)\b")
    problems = []
    checked = 0
    touched = set()
    for path, src in sources.items():
        body = strip_noise(src)
        for m in ours.finditer(body):
            enum, const = m.group(1), m.group(2)
            checked += 1
            if const in enums[enum]:
                continue
            line = body[: m.start()].count("\n") + 1
            problems.append(
                "%s:%d: %s.%s does not exist - %s declares %s"
                % (path, line, enum, const, enum,
                   ", ".join(sorted(enums[enum]))))
        for m in theirs.finditer(body):
            obj, member = m.group(1), m.group(2)
            if obj in enums:
                continue          # ours, already judged above
            checked += 1
            touched.add(obj)
            if member in FRAMEWORK[obj]:
                continue
            line = body[: m.start()].count("\n") + 1
            problems.append(
                "%s:%d: %s.%s does not exist - %s has %s"
                % (path, line, obj, member, obj,
                   ", ".join(sorted(FRAMEWORK[obj]))))

    # A table nobody consults is a table that will be wrong when someone
    # finally does. Say so rather than carry it quietly.
    for obj in sorted(set(FRAMEWORK) - touched - set(enums)):
        problems.append(
            "FRAMEWORK lists %s, and nothing in the project references it - "
            "drop the entry or the table starts drifting" % obj)

    for p in sorted(set(problems)):
        print("  ERROR " + p)
    print("%d enums + %d framework objects, %d member references checked"
          % (len(enums), len(FRAMEWORK), checked))
    if problems:
        print("FAILED: %d bad reference(s)" % len(set(problems)))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
