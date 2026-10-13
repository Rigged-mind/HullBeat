"""Which languages HullBeat ships, found the one way that cannot drift: on disk.

Every check used to name `values-uk` itself. That was fine for one
translation and is exactly how a second one slips through: add `values-de`,
and check_translations, validate_plurals and the generators keep comparing
English with Ukrainian, all green, while German falls back to English
silently. So no check keeps its own list - they all ask this module.

A locale is a `values-<tag>` folder holding a `strings.xml`. `values-night`
and other qualifier folders hold colours, not text, and are not locales.

English is the base (`values/`) and never appears in the list.
"""
import os
import re

RES = os.path.join("app", "src", "main", "res")

# values-uk, values-de, values-es, values-pt-rBR - not values-night.
LOCALE_DIR = re.compile(r"^values-([a-z]{2,3}(?:-r[A-Z]{2})?)$")

# CLDR plural categories each locale's <plurals> must carry. Android picks the
# category with the device's ICU and falls back to `other` for a missing one,
# so a missing `few` in Ukrainian renders "5 робота" without any warning.
#
# es: CLDR 42 (ICU 72, Android 14) added `many` for millions ("1 000 000 de
# horas"). Older devices never ask for it; newer ones would fall back to
# `other`, which reads the same - but the form is required here so that the
# Spanish file is complete for the rule the device actually runs.
PLURAL_FORMS = {
    "": {"one", "other"},
    "uk": {"one", "few", "many", "other"},
    "de": {"one", "other"},
    "es": {"one", "many", "other"},
}


def resource_locales(res=RES):
    """Tags of every translated locale under res/, sorted."""
    found = []
    for name in os.listdir(res):
        m = LOCALE_DIR.match(name)
        if m and os.path.exists(os.path.join(res, name, "strings.xml")):
            found.append(m.group(1))
    return sorted(found)


def values_dir(locale, res=RES):
    return os.path.join(res, "values" if not locale else f"values-{locale}")


def review_priority(locale):
    """Markdown block of "check these first" questions for a reviewer.

    A reviewer facing 700 rows stops reading closely somewhere in the second
    hundred, so the questions where a wrong word costs most go on top. They
    live in catalog/i18n/<locale>.json under `_reviewPriority`, each naming the
    keys it is about; the current draft of each key is looked up here, so the
    block can never quote a word the glossary has since changed.

    Key forms: cat:<code>, cmp:<code>, chk:<chk_key>, hint:<English label>,
    syn:<component code>, ui:<string name in catalog/i18n/ui/values-<locale>>.
    An unknown key raises: a question pointing at nothing is worse than none.
    """
    import io
    import json
    path = os.path.join("catalog", "i18n", f"{locale}.json")
    if not os.path.exists(path):
        return []
    glossary = json.load(io.open(path, encoding="utf-8"))
    items = glossary.get("_reviewPriority") or []
    if not items:
        return []

    ui = {}
    ui_path = os.path.join("catalog", "i18n", "ui", f"values-{locale}", "strings.xml")
    if os.path.exists(ui_path):
        src = re.sub(r"<!--.*?-->", "", io.open(ui_path, encoding="utf-8").read(), flags=re.S)
        ui = dict(re.findall(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', src, re.S))

    def term(block, key):
        v = glossary.get(block, {}).get(key)
        return v[0] if isinstance(v, list) else None

    def lookup(ref):
        kind, _, key = ref.partition(":")
        found = {
            "cat": lambda: term("categories", key),
            "cmp": lambda: term("components", key),
            "chk": lambda: term("checklists", key),
            "hint": lambda: term("specHints", key),
            "syn": lambda: "; ".join(glossary.get("synonyms", {}).get(key, [])) or None,
            "ui": lambda: ui.get(key),
        }.get(kind, lambda: None)()
        if found is None:
            raise SystemExit(f"{path}: _reviewPriority names '{ref}', which has no draft")
        return found

    lines = ["\n## Check these first\n",
             "\nThe places where a wrong word costs most. Each names the rows it is "
             "about, with the current draft.\n\n"]
    for n, item in enumerate(items, 1):
        lines.append(f"{n}. **{item['question']}**\n")
        for ref in item["keys"]:
            draft = lookup(ref).replace("|", "\\|").replace("\n", " ")
            lines.append(f"   - `{ref}` - {draft}\n")
    lines.append("\n")
    return lines
