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
