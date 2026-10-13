#!/usr/bin/env python3
"""Three lists of languages must be one list.

  * res/xml/locales_config.xml - what Android's per-app language picker offers
  * AppLanguage.SUPPORTED      - what HullBeat's own settings offer
  * values-<tag>/strings.xml   - what is actually translated (plus English)

They are written by hand in three places, so they drift, and every direction
of drift is silent:

  * a folder with no config entry: the translation reaches phones whose
    SYSTEM language matches, but nobody else can choose it;
  * a config entry with no folder: the picker offers a language and the app
    answers in English;
  * SUPPORTED out of step: HullBeat's settings and Android's settings
    disagree about which languages exist.

The release gate for a new language is the `values-<tag>` folder itself:
merged, it ships to every phone set to that language. This check makes sure
that once the folder is in, the other two lists follow it.

Run:  python tools/check_locales.py
Exit: 1 if the three lists differ.
"""
import io
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import locales

CONFIG = os.path.join(locales.RES, "xml", "locales_config.xml")
APP_LANGUAGE = os.path.join("app", "src", "main", "java", "app", "hullbeat",
                            "ui", "AppLanguage.kt")


def config_tags():
    src = io.open(CONFIG, encoding="utf-8").read()
    src = re.sub(r"<!--.*?-->", "", src, flags=re.S)
    return re.findall(r'<locale\s+android:name="([^"]+)"', src)


def supported_tags():
    src = io.open(APP_LANGUAGE, encoding="utf-8").read()
    consts = dict(re.findall(r'const val (\w+) = "([^"]+)"', src))
    m = re.search(r"val SUPPORTED[^=]*=\s*listOf\(([^)]*)\)", src)
    if not m:
        return None
    out = []
    for item in (x.strip() for x in m.group(1).split(",") if x.strip()):
        out.append(item.strip('"') if item.startswith('"') else consts.get(item, item))
    return out


def main():
    shipped = set(locales.resource_locales()) | {"en"}
    config = config_tags()
    supported = supported_tags()

    problems = []
    if supported is None:
        problems.append(f"{APP_LANGUAGE}: no `val SUPPORTED = listOf(...)` found")
    for name, tags in (("locales_config.xml", config), ("AppLanguage.SUPPORTED", supported)):
        if tags is None:
            continue
        if len(tags) != len(set(tags)):
            problems.append(f"{name} lists a language twice: {tags}")
        for tag in sorted(shipped - set(tags)):
            problems.append(f"{name} does not offer '{tag}', which is translated")
        for tag in sorted(set(tags) - shipped):
            problems.append(f"{name} offers '{tag}', which has no values-{tag}/strings.xml")
    if config and supported and config[0] != supported[0]:
        problems.append(f"default differs: locales_config.xml starts with "
                        f"'{config[0]}', AppLanguage.SUPPORTED with '{supported[0]}'")

    for p in problems:
        print("  ERROR " + p)
    print(f"languages: translated {sorted(shipped)}, config {config}, "
          f"settings {supported}")
    if problems:
        print(f"FAILED: {len(problems)} problem(s)")
        return 1
    print("OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
