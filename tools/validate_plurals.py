#!/usr/bin/env python3
"""Check the quantity strings, including the two mistakes that are silent.

Neither of these fails a build on its own, which is why they ship:

  * a Slavic locale given only one/other. Android then picks `one` for 5 and
    renders "5 робота". Lint warns about the missing `other`, never about a
    missing `few`/`many` - those are simply absent, and absent is legal.
  * a verb left outside the plural. "%d позначка" + " створять запис" is two
    correct halves that make "21 позначка створять".

The second cannot be checked mechanically in general, but its signature can:
in a locale with `few`, a resource whose forms differ ONLY in the counted noun
and nowhere else is suspicious when the phrase contains a verb.

Run:  python tools/validate_plurals.py
Exit: 1 on a missing form or a name present in one locale and not the other.
"""
import io
import re
import glob
import os
import sys
from xml.etree import ElementTree

RES = os.path.join("app", "src", "main", "res")
# CLDR quantity categories that must be present, per locale.
REQUIRED = {
    "": {"one", "other"},
    "uk": {"one", "few", "many", "other"},
}


def load(locale):
    name = "values" if not locale else f"values-{locale}"
    path = os.path.join(RES, name, "plurals.xml")
    if not os.path.exists(path):
        return None, path
    root = ElementTree.parse(path).getroot()
    out = {}
    for pl in root.findall("plurals"):
        out[pl.get("name")] = {
            item.get("quantity"): (item.text or "") for item in pl.findall("item")
        }
    return out, path


def main():
    failed = []
    sets = {}
    for locale, required in REQUIRED.items():
        data, path = load(locale)
        if data is None:
            print(f"    MISSING FILE: {path}")
            failed.append(path)
            continue
        sets[locale] = data
        for name, forms in sorted(data.items()):
            missing = required - set(forms)
            if missing:
                print(f"    {path}: '{name}' has no {sorted(missing)}")
                failed.append(name)
            extra = set(forms) - required
            if extra:
                print(f"    {path}: '{name}' has unexpected {sorted(extra)}")
                failed.append(name)
        print(f"{path:44} {len(data)} plurals")

    if len(sets) == len(REQUIRED):
        base = set(sets[""])
        for locale, data in sets.items():
            if locale and set(data) != base:
                only = sorted(set(data) ^ base)
                print(f"    values-{locale} and values differ: {only}")
                failed.extend(only)

    # The verb-outside-the-plural signature: `one` and `few` differing in
    # exactly one word means everything else stayed put, and a verb that
    # stayed put is the "21 позначка створять" bug.
    #
    # The first version of this warned on five of seven resources, all of them
    # correct - which is worse than no check, because a linter that cries wolf
    # gets switched off. Every false positive shared one trait: the invariant
    # word was an impersonal past passive in -но/-то - «пропущено»,
    # «прострочено», «створено», «записано». Those genuinely never agree with
    # number, so they are exactly what the check must not flag. What remains
    # flagged is a personal verb form, which is the real bug.
    IMPERSONAL = ("но", "то")
    for name, forms in sorted(sets.get("uk", {}).items()):
        one, few = forms.get("one", "").split(), forms.get("few", "").split()
        if len(one) != len(few) or len(one) < 3:
            continue
        diff = [i for i, (a, b) in enumerate(zip(one, few)) if a != b]
        if len(diff) != 1:
            continue
        invariant = [w.strip(".,;:»«") for i, w in enumerate(one) if i != diff[0]]
        if any(w.lower().endswith(IMPERSONAL) for w in invariant):
            continue        # impersonal -но/-то: correctly invariant
        # Function words carry no agreement, so a phrase whose only invariants
        # are short ones ("за %d мотогодину") has nothing to get wrong.
        if not any(len(w) >= 4 for w in invariant):
            continue
        print(f"    WARN values-uk: '{name}' - one and few differ only in "
              f"{one[diff[0]]!r}/{few[diff[0]]!r}; check that no personal verb "
              "form was left uninflected")

    failed += check_counted_nouns()
    failed += check_prototype(sets.get("uk", {}))

    if failed:
        print(f"\nFAILED: {len(failed)} problem(s)")
        return 1
    print("OK")
    return 0


# Words that follow a number and do not agree with it.
#
#   -но / -то   the impersonal past: «прострочено», «імпортовано». It is the
#               one Ukrainian form that never inflects, which is why the
#               checklists lean on it - see docs/i18n-uk.md.
#   the rest    adverbs, units and abbreviations.
INVARIANT_AFTER_NUMBER = {
    "скоро", "шт", "год", "км", "кг", "г", "л", "м", "мм", "см",
    "раз", "разів", "хв", "с",
}

COUNTED_RE = re.compile(r"%(?:\d+\$)?d\s+([а-яіїєґА-ЯІЇЄҐ][а-яіїєґ\'\u02bc-]*)")
STRING_RE = re.compile(r"<string name=\"([^\"]+)\">(.*?)</string>", re.S)


def check_counted_nouns():
    """A number in a plain <string>, followed by a word that must agree.

    `validate_plurals` used to check only the <plurals> that exist. It could
    not see the ones that SHOULD exist, so this shipped:

        <string name="departure_check_chip">Вихід у море · %d пунктів</string>

    which is right for five items and wrong for four - and the count comes
    from the vessel profile, so whether the bug is visible depends on the
    boat.

    A number at the end of a string, or before an impersonal -но/-то, or
    before a unit, agrees with nothing and is left alone. Of the fourteen
    numbered strings in this project, that rule flagged exactly the four
    that were wrong.
    """
    failed = []
    for locale_dir in sorted(glob.glob(os.path.join(RES, "values-uk*"))):
        path = os.path.join(locale_dir, "strings.xml")
        if not os.path.exists(path):
            continue
        text = io.open(path, encoding="utf-8").read()
        for match in STRING_RE.finditer(text):
            for word in COUNTED_RE.findall(match.group(2)):
                low = word.lower()
                if low.endswith(("но", "то")) or low in INVARIANT_AFTER_NUMBER:
                    continue
                failed.append(match.group(1))
                print(f"    '{match.group(1)}' puts %d before {word!r}, which "
                      f"has to agree with it - this needs <plurals>, not "
                      f"<string>")
    print(f"{'counted nouns in plain strings':44} "
          f"{len(failed)} needing plurals")
    return failed


# Forms the prototype needs that no Android resource covers yet. Anything not
# on this list has to exist in values-uk/plurals.xml, or the two surfaces have
# started to drift - the same failure that put "джекштоки" in the checklists
# while the catalog already said otherwise.
PROTO_ONLY = {("рік", "роки", "років")}


def check_prototype(uk):
    """The prototype hardcodes its own plural triples in JS.

    Two copies of one rule is exactly what this project keeps getting wrong,
    so rather than let the JS and the XML diverge quietly, every triple in the
    prototype must be spelled the same way somewhere in values-uk.
    """
    import re
    path = os.path.join("prototype", "index.html")
    if not os.path.exists(path):
        return []
    html = io.open(path, encoding="utf-8").read()
    blob = " ".join(" ".join(f.values()) for f in uk.values())
    bad = []
    triples = re.findall(
        r'plural\([^,]+,\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\)', html)
    for triple in set(triples):
        if triple in PROTO_ONLY:
            continue
        missing = [w for w in triple if w not in blob]
        if missing:
            print(f"    prototype uses {triple} but values-uk has no "
                  f"{missing} - the two copies have drifted")
            bad.append(str(triple))
    print(f"{path:44} {len(set(triples))} plural triples, "
          f"{len(PROTO_ONLY)} allowed to be prototype-only")
    return bad


if __name__ == "__main__":
    sys.exit(main())
