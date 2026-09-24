#!/usr/bin/env python3
"""Every base-locale string must have a Ukrainian counterpart.

This is the one localisation failure Android does NOT report. A missing
translation is not an error, not a warning, not a lint by default: the
resource resolver simply falls back to `values/`, and a Ukrainian owner reads
English. On an app whose whole positioning is being the one boat log that
speaks Ukrainian properly, a silent fallback is worse than a crash.

It bites hardest exactly when the work is going well. Writing HullBeatApp
needed a notification channel name, `values/strings.xml` already had one, and
`values-uk/strings.xml` did not exist at all - so the first thing the owner
would have seen from the app was an English channel in Android's own settings.

The reverse direction matters too: a name in `values-uk/` with nothing in
`values/` resolves to nothing at all, because the base locale is what defines
the resource.

ALLOWED_UNTRANSLATED is a list, not a threshold. A product name is a proper
noun and stays in Latin script in every locale (docs/i18n-uk.md §4); anything
else appearing here is a decision someone has to make on purpose.

Run:  python tools/check_translations.py
Exit: 1 on any untranslated or orphaned name.
"""
import glob
import io
import os
import re
import sys

BASE = os.path.join("app", "src", "main", "res", "values")
UK = os.path.join("app", "src", "main", "res", "values-uk")

# HullBeat is a proper noun. `freeboard` used to sit in the do-not-translate
# list for the same reason and was released back to meaning надводний борт
# when the app was renamed.
ALLOWED_UNTRANSLATED = {"app_name"}

NAME_RE = re.compile(r"<(?:string|plurals)\s+name=\"([^\"]+)\"")


def names(folder):
    found = {}
    for path in sorted(glob.glob(os.path.join(folder, "*.xml"))):
        src = io.open(path, encoding="utf-8").read()
        for n in NAME_RE.findall(src):
            found[n] = os.path.basename(path)
    return found


def main():
    base = names(BASE)
    uk = names(UK)

    missing = sorted(set(base) - set(uk) - ALLOWED_UNTRANSLATED)
    orphan = sorted(set(uk) - set(base))
    # An allowlist entry that is no longer untranslated is stale bookkeeping.
    stale = sorted(ALLOWED_UNTRANSLATED & set(uk))

    for n in missing:
        print("  ERROR %s is in values/%s with no values-uk translation - "
              "Android falls back to English silently" % (n, base[n]))
    for n in orphan:
        print("  ERROR %s is in values-uk/%s but not in the base locale, so it "
              "resolves to nothing" % (n, uk[n]))
    for n in stale:
        print("  ERROR %s is on ALLOWED_UNTRANSLATED and now HAS a Ukrainian "
              "value - drop it from the list" % n)

    ok = len(base) - len(missing) - len(ALLOWED_UNTRANSLATED & set(base))
    print("%s   %d of %d translated, %d deliberately not"
          % (UK, ok, len(base), len(ALLOWED_UNTRANSLATED & set(base))))
    bad = len(missing) + len(orphan) + len(stale)
    if bad:
        print("FAILED: %d problem(s)" % bad)
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
