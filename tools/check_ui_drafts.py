#!/usr/bin/env python3
"""Interface strings for a language that is not released yet.

Draft translations of values/strings.xml and values/plurals.xml live in
catalog/i18n/ui/values-<tag>/, in the same Android format, and NOT under
res/: a values-<tag> folder in res/ ships to every phone set to that language
the moment it is merged, reviewed or not. Releasing a language is therefore
one move - `git mv catalog/i18n/ui/values-de app/src/main/res/values-de` -
together with dropping "_status": "draft" from catalog/i18n/de.json and
adding the tag to AppLanguage.SUPPORTED and locales_config.xml (which
check_locales.py then holds together).

Until then nothing checks these files, because no Android tool sees them. So
this does what aapt and the runtime would:

  * placeholders: every %s / %1$d / %.1f of the English string must be in the
    translation, with the same index and conversion. A missing or retyped one
    is not a typo, it is an IllegalFormatException or a wrong number on screen;
  * escapes: an unescaped ' fails aapt; an unescaped " is silently stripped;
  * plurals: the CLDR forms of tools/locales.py PLURAL_FORMS, no more, no less;
  * keys: a key the base does not have is an error; a key not yet translated
    is reported, not failed - drafts are filled over time.

It writes catalog/i18n/<tag>-ui-review.md, English beside the draft, for the
reviewer.

Run:  python tools/check_ui_drafts.py
Exit: 1 on a placeholder, escape, plural-form or orphan-key error.
"""
import io
import os
import re
import sys
from xml.etree import ElementTree

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import locales

DRAFTS = os.path.join("catalog", "i18n", "ui")
DRAFT_DIR = re.compile(r"^values-([a-z]{2,3}(?:-r[A-Z]{2})?)$")

# Only what this project formats with: %s %d %f, optionally %1$ and %.1.
# "100% offline" must not count, so no flags and no space are allowed.
SPEC = re.compile(r"%(?:(\d+)\$)?(?:\.\d+)?([sdf])")


def specs(text):
    """Placeholders as a sorted list of (index, conversion)."""
    out, seq = [], 0
    for idx, conv in SPEC.findall(text or ""):
        if idx:
            out.append((int(idx), conv))
        else:
            seq += 1
            out.append((seq, conv))
    return sorted(out)


def raw_texts(path, tag):
    """name -> raw inner text, straight from the file (escapes intact)."""
    src = io.open(path, encoding="utf-8").read()
    src = re.sub(r"<!--.*?-->", "", src, flags=re.S)
    return dict(re.findall(r'<%s\s+name="([^"]+)"[^>]*>(.*?)</%s>' % (tag, tag), src, re.S))


def load(folder):
    strings, plurals = {}, {}
    s = os.path.join(folder, "strings.xml")
    if os.path.exists(s):
        ElementTree.parse(s)  # well-formed, or this raises
        strings = raw_texts(s, "string")
    p = os.path.join(folder, "plurals.xml")
    if os.path.exists(p):
        root = ElementTree.parse(p).getroot()
        for pl in root.findall("plurals"):
            plurals[pl.get("name")] = {
                it.get("quantity"): (it.text or "") for it in pl.findall("item")}
    return strings, plurals


def escape_problems(text):
    """What aapt does to a raw string value."""
    bad = []
    if re.search(r"(?<!\\)'", text):
        bad.append("unescaped ' (aapt error) - write \\'")
    if re.search(r'(?<!\\)"', text):
        bad.append('unescaped " (aapt strips it) - write \\"')
    if text[:1] in ("@", "?"):
        bad.append("starts with @ or ? (read as a reference)")
    return bad


def md(text):
    return (text or "").replace("|", "\\|").replace("\n", " ")


def check_locale(tag, base_s, base_p, folder=None):
    """Check one language's interface strings and write its review list.

    `folder` is the draft in catalog/i18n/ui/ by default, or res/values-<tag>
    for a language released while its review is still open.
    """
    released = folder is not None
    folder = folder or os.path.join(DRAFTS, f"values-{tag}")
    errors = []
    if tag not in locales.PLURAL_FORMS:
        errors.append(f"no CLDR plural rule for '{tag}' in tools/locales.py PLURAL_FORMS")
    if not released and os.path.exists(os.path.join(locales.RES, f"values-{tag}")):
        errors.append(f"res/values-{tag} also exists - a released language has no draft; "
                      "move the folder, do not keep two copies")

    try:
        strings, plurals = load(folder)
    except ElementTree.ParseError as e:
        return [f"{folder}: not well-formed XML - {e}"], 0

    allowed = {"app_name"}  # a proper noun, see check_translations.py
    for name in sorted(set(strings) - set(base_s)):
        errors.append(f"strings.xml: '{name}' is not in values/strings.xml")
    for name in sorted(set(plurals) - set(base_p)):
        errors.append(f"plurals.xml: '{name}' is not in values/plurals.xml")

    for name, text in sorted(strings.items()):
        if name not in base_s:
            continue
        if specs(text) != specs(base_s[name]):
            errors.append(f"'{name}': placeholders {specs(text)} but English has "
                          f"{specs(base_s[name])}")
        errors += [f"'{name}': {p}" for p in escape_problems(text)]

    required = locales.PLURAL_FORMS.get(tag, set())
    for name, forms in sorted(plurals.items()):
        if name not in base_p:
            continue
        if set(forms) != required:
            errors.append(f"plural '{name}': forms {sorted(forms)}, {tag} needs {sorted(required)}")
        want = specs(base_p[name].get("other", ""))
        for q, text in forms.items():
            if specs(text) != want:
                errors.append(f"plural '{name}' [{q}]: placeholders {specs(text)}, "
                              f"English has {want}")
            errors += [f"plural '{name}' [{q}]: {p}" for p in escape_problems(text)]

    missing_s = sorted(set(base_s) - set(strings) - allowed)
    missing_p = sorted(set(base_p) - set(plurals))

    out = os.path.join("catalog", "i18n", f"{tag}-ui-review.md")
    lines = [f"# Interface strings to review - {tag}\n",
             f"\n{len(strings)} strings and {len(plurals)} plurals drafted"
             + (f"; {len(missing_s) + len(missing_p)} not drafted yet" if missing_s or missing_p else "")
             + ". None is confirmed. Check two things on each row: is it what a "
             "sailor would say, and does it fit a phone screen? Navigation tabs "
             "and buttons have the least room.\n",
             "\n`%s`, `%1$s`, `%d` are filled in by the app; keep them, move them "
             "freely within the sentence.\n"]
    # The same "check these first" block as <tag>-review.md: some of its
    # questions are about interface words, and a reviewer may open either file.
    lines += locales.review_priority(tag)
    lines += ["\n## All strings\n",
              "\n| Key | English | Draft | Correction |\n|---|---|---|---|\n"]
    for name in base_s:
        if name in strings:
            lines.append(f"| `{name}` | {md(base_s[name])} | **{md(strings[name])}** | |\n")
    for name in base_p:
        if name in plurals:
            en = " / ".join(f"{q}: {md(t)}" for q, t in base_p[name].items())
            tr = " / ".join(f"{q}: {md(t)}" for q, t in plurals[name].items())
            lines.append(f"| `{name}` | {en} | **{tr}** | |\n")
    io.open(out, "w", encoding="utf-8", newline="\n").writelines(lines)

    print(f"{folder:34} {len(strings)} strings, {len(plurals)} plurals, "
          f"{len(missing_s)} + {len(missing_p)} not drafted -> {out}")
    for e in errors:
        print("  ERROR " + e)
    return errors, len(missing_s) + len(missing_p)


def under_review_released(drafts):
    """Tags shipped in res/ whose glossary still has a term not marked `ok`.

    Ukrainian is left out: its interface was reviewed before this tooling
    existed, and its open terms live in uk-review.md alone.
    """
    import json
    out = []
    for name in sorted(os.listdir(os.path.join("catalog", "i18n"))):
        tag, ext = os.path.splitext(name)
        if ext != ".json" or tag in ("en", "uk") or tag in drafts:
            continue
        if not os.path.exists(os.path.join(locales.values_dir(tag), "strings.xml")):
            continue
        g = json.load(io.open(os.path.join("catalog", "i18n", name), encoding="utf-8"))
        statuses = [v[1] for block in ("categories", "components", "checklists",
                                       "specHints", "profile")
                    for v in (g.get(block) or {}).values() if isinstance(v, list)]
        if any(s != "ok" for s in statuses):
            out.append(tag)
    return out


def main():
    base_s = raw_texts(os.path.join(locales.values_dir(""), "strings.xml"), "string")
    base_p = load(locales.values_dir(""))[1]
    drafts = []
    if os.path.isdir(DRAFTS):
        drafts = sorted(m.group(1) for m in (DRAFT_DIR.match(n) for n in os.listdir(DRAFTS)) if m)
    failed = 0
    for tag in drafts:
        errors, _ = check_locale(tag, base_s, base_p)
        failed += len(errors)
    # A language can be released before its review is done (German and
    # Spanish were). Its interface review list must keep up with res/, or
    # the reviewer would be correcting strings the app no longer shows.
    for tag in under_review_released(drafts):
        errors, _ = check_locale(tag, base_s, base_p, locales.values_dir(tag))
        failed += len(errors)
    if not drafts and not under_review_released(drafts):
        print("no interface drafts, no released language under review")
    if failed:
        print(f"FAILED: {failed} problem(s)")
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
