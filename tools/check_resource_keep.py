#!/usr/bin/env python3
"""A resource looked up by name must be protected from resource shrinking.

WHY THIS EXISTS

`Component.displayName` resolves every component name through

    getIdentifier("cmp_$catalogCode", "string", packageName)

because there are 329 of them and nothing can reference each one literally.
The release build sets `isShrinkResources = true`, and a dynamic lookup is
invisible to the shrinker: without `res/raw/keep.xml` it strips all 350
catalog strings - 329 components and 21 categories - in both locales.

The failure mode is the worst kind: configuration-specific and silent. Debug
builds keep every resource, so the tree reads perfectly through development,
and the release APK shows a boat with 253 blank rows. Nobody finds that until
a tester installs a signed build.

The check pairs the two halves: every `getIdentifier("prefix_...")` in Kotlin
must have a matching `@string/prefix_*` in the keep file, and the keep file
must exist at all whenever shrinking is on.

Run:  python tools/check_resource_keep.py
Exit: 1 if a dynamic lookup is unprotected.
"""
import glob
import io
import os
import re
import sys

GRADLE = os.path.join("app", "build.gradle.kts")
KEEP = os.path.join("app", "src", "main", "res", "raw", "keep.xml")
KOTLIN = os.path.join("app", "src", "main", "java")

# getIdentifier("cmp_$code", "string", …) - the literal prefix before the
# first interpolation is what the keep rule has to cover.
LOOKUP_RE = re.compile(r'getIdentifier\(\s*"([a-z_]+?)_?\$')


def main():
    gradle = io.open(GRADLE, encoding="utf-8").read()
    shrinking = re.search(r"isShrinkResources\s*=\s*true", gradle) is not None

    prefixes = {}
    for path in glob.glob(os.path.join(KOTLIN, "**", "*.kt"), recursive=True):
        src = io.open(path, encoding="utf-8").read()
        for m in LOOKUP_RE.finditer(src):
            prefixes.setdefault(m.group(1), []).append(
                "%s:%d" % (path, src[: m.start()].count("\n") + 1))

    problems = []
    keep = io.open(KEEP, encoding="utf-8").read() if os.path.exists(KEEP) else None

    if prefixes and shrinking and keep is None:
        problems.append(
            "%s does not exist, but release shrinks resources and %d prefix(es) "
            "are looked up by name: %s"
            % (KEEP, len(prefixes), ", ".join(sorted(prefixes))))
    elif keep is not None:
        kept = set(re.findall(r"@string/([a-z_]+)\*", keep))
        for prefix, sites in sorted(prefixes.items()):
            if prefix + "_" not in kept and prefix not in kept:
                problems.append(
                    '"%s_*" is resolved with getIdentifier at %s but %s does '
                    "not keep it - the release build will strip those strings "
                    "and the screen goes blank"
                    % (prefix, sites[0], KEEP))

    for p in problems:
        print("  ERROR " + p)
    print("%s   %d dynamic prefix(es), shrinking %s"
          % (KEEP if keep else "(no keep file)", len(prefixes),
             "on" if shrinking else "off"))
    if problems:
        print("FAILED: %d unprotected lookup(s)" % len(problems))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
