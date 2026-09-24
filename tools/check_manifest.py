#!/usr/bin/env python3
"""Every class the manifest declares must exist as a source file.

The manifest declared three components and none of them had ever been written:

    android:name=".HullBeatApp"            -> ClassNotFoundException, process
                                              dies before any UI appears
    android:name=".MainActivity"           -> nothing to launch
    android:name=".notify.MarkDoneReceiver" -> crash on the notification's own
                                              "Зроблено" button, which is
                                              feature #1 in ui-spec.md §7

Nothing complained, because a manifest is data: the build packages whatever
string is in it and the failure waits for the device. That is the same shape as
every other defect in this project - declared in one place, not carried to the
last point of use. `check_entities.py` guards the Room half of it; this guards
the manifest half.

Resolution follows the Android rules: a name starting with "." is relative to
the manifest package (the Gradle `namespace`), a name with no dot at all is
also relative, and a fully qualified name is used as is.

Run:  python tools/check_manifest.py
Exit: 1 if a declared class has no source file.
"""
import io
import os
import re
import sys

MANIFEST = os.path.join("app", "src", "main", "AndroidManifest.xml")
GRADLE = os.path.join("app", "build.gradle.kts")
SRC_ROOTS = [os.path.join("app", "src", "main", "java"),
             os.path.join("app", "src", "main", "kotlin")]

# Every manifest element whose android:name is a class we must be able to find.
# `activity-alias` is deliberately absent: its android:name is a synthetic name,
# and android:targetActivity is the real class - handled below.
CLASS_ELEMENTS = ("application", "activity", "receiver", "service", "provider")

# Names this app used to have. A rename that leaves one behind is not
# cosmetic when the file carrying it generates build configuration.
DEAD_PACKAGES = ("log.sailboat", "log/sailboat",
                 "app.freeboard", "app/freeboard")


def namespace():
    src = io.open(GRADLE, encoding="utf-8").read()
    m = re.search(r'namespace\s*=\s*"([^"]+)"', src)
    return m.group(1) if m else None


def declared(xml):
    """(element, raw android:name) for every class-bearing element."""
    out = []
    for el in CLASS_ELEMENTS:
        for m in re.finditer(r"<%s\b([^>]*)>" % el, xml, re.S):
            attrs = m.group(1)
            n = re.search(r'android:name\s*=\s*"([^"]+)"', attrs)
            if n:
                out.append((el, n.group(1)))
    for m in re.finditer(r'android:targetActivity\s*=\s*"([^"]+)"', xml):
        out.append(("activity-alias", m.group(1)))
    return out


def resolve(name, pkg):
    """Manifest class name -> fully qualified name."""
    if name.startswith("."):
        return pkg + name
    if "." not in name:
        return pkg + "." + name
    return name


def source_exists(fqcn):
    rel = os.path.join(*fqcn.split("."))
    for root in SRC_ROOTS:
        for ext in (".kt", ".java"):
            if os.path.exists(os.path.join(root, rel + ext)):
                return True
    return False


def main():
    pkg = namespace()
    if not pkg:
        print("could not read `namespace` from " + GRADLE)
        return 1
    xml = io.open(MANIFEST, encoding="utf-8").read()

    rows = declared(xml)
    missing = []
    for el, raw in rows:
        fqcn = resolve(raw, pkg)
        if not source_exists(fqcn):
            missing.append((el, raw, fqcn))

    # --- and nothing anywhere may name a package the app no longer uses ----
    # HullBeat has been renamed twice: log.sailboat -> app.freeboard ->
    # app.hullbeat. The second rename swept `freeboard` out of 29 files and
    # left three `log.sailboat` behind, because nobody grepped for a name that
    # had already been dead for a rename. One of them was in scaffold_build.py,
    # which GENERATES build.gradle.kts - re-running it would have quietly
    # rewritten the namespace back to a value two renames old.
    stale = []
    for dirpath, dirnames, filenames in os.walk("."):
        dirnames[:] = [d for d in dirnames
                       if d not in (".git", "build", ".gradle", ".idea", "pages")]
        for name in filenames:
            if os.path.splitext(name)[1] not in (".kt", ".kts", ".py", ".xml",
                                                 ".md", ".json", ".pro"):
                continue
            fp = os.path.join(dirpath, name)
            # This file has to spell the dead names out to look for them, and
            # a checker that reports itself is a checker nobody runs twice.
            if os.path.abspath(fp) == os.path.abspath(__file__):
                continue
            try:
                src = io.open(fp, encoding="utf-8").read()
            except (UnicodeDecodeError, OSError):
                continue
            for dead in DEAD_PACKAGES:
                if dead in src:
                    line = src[: src.index(dead)].count("\n") + 1
                    stale.append("%s:%d: names the retired package '%s'; this "
                                 "app is '%s'" % (fp, line, dead, pkg))
    for line in stale:
        print("  ERROR " + line)
    for el, raw, fqcn in missing:
        print('  ERROR <%s android:name="%s"> resolves to %s, and no .kt or '
              ".java file defines it - the build packages the string and the "
              "crash waits for the device" % (el, raw, fqcn))

    print("%s   %d declared class(es), %d missing, %d stale package name(s)"
          % (MANIFEST, len(rows), len(missing), len(stale)))
    if missing or stale:
        print("FAILED: %d problem(s)" % (len(missing) + len(stale)))
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
