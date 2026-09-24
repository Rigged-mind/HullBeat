#!/usr/bin/env python3
"""Rebuild the search index embedded in prototype/index.html from the catalog.

The prototype carries its own copy of the 321 components so it can run as a
single file with no server. A copy is a copy: rename a term in the glossary and
the prototype keeps showing the old one until this runs. That is the same drift
that put "джекштоки" in the checklists while the catalog already said
"Страхувальні лінійні леєри", so the copy gets generated, never hand-edited.

Run:  python tools/make_prototype_data.py
Exit: 1 if the blob cannot be located or the counts look wrong.
"""
import io
import json
import os
import re
import sys

from uk_plural import phrase

CATALOG = os.path.join("catalog", "catalog.json")
UK = os.path.join("catalog", "i18n", "uk.json")
EN = os.path.join("catalog", "i18n", "en.json")
CHECKLISTS = os.path.join("catalog", "checklists.json")
PROTO = os.path.join("prototype", "index.html")

CRIT = {"low": 0, "med": 1, "high": 2}


def main():
    catalog = json.load(io.open(CATALOG, encoding="utf-8"))
    uk = json.load(io.open(UK, encoding="utf-8"))
    en = json.load(io.open(EN, encoding="utf-8"))

    # The tree needs the categories themselves: their order, their icon, and
    # the rule that decides whether this boat has them at all.
    cats = []
    for cat in catalog["categories"]:
        row = {"c": cat["code"], "n": uk["categories"][cat["code"]][0],
               "icon": cat.get("icon", ""), "sort": cat.get("sort", 0)}
        if cat.get("appliesTo"):
            row["r"] = cat["appliesTo"]
        if cat.get("perEngine"):
            row["per"] = True
        cats.append(row)
    cats.sort(key=lambda x: x["sort"])

    items = []
    for cat in catalog["categories"]:
        group = uk["categories"][cat["code"]][0]
        for c in cat["components"]:
            code = c["code"]
            # Both locales feed one index: an owner with a Ukrainian interface
            # who learned from English manuals types "Racor", and must find it.
            aliases = sorted(set(uk["synonyms"].get(code, []))
                             | set(en["synonyms"].get(code, [])),
                             key=str.lower)
            row = {
                "c": code,
                "n": uk["components"][code][0],
                "e": c["en"],
                "g": group,
                "cat": cat["code"],
                "k": CRIT.get(c.get("crit", "low"), 0),
                "a": aliases,
            }
            # The node's own rule, so the tree filters as strictly as the test.
            if c.get("appliesTo"):
                row["r"] = c["appliesTo"]
            if c.get("perEngine") is False:
                row["shared"] = True
            if c.get("parent"):
                row["parent"] = c["parent"]
            # The component page needs these to say anything true about a node
            # it has no demo data for: the interval it is due on, and the
            # passport rows the owner is expected to fill in.
            if c.get("sched"):
                row["s"] = c["sched"]
            if c.get("specHints"):
                # Translated labels, not the English source: the passport is
                # the last block that still read in English on a uk screen.
                # entries are ["text", "ok" | "check"]; older ones were bare
                row["h"] = [(lambda v: v[0] if isinstance(v, list) else v)(
                    uk["specHints"].get(x, x)) for x in c["specHints"]]
            if c.get("expires"):
                row["x"] = True
            items.append(row)

    # 113 checklist items existed only as JSON: no screen had ever rendered
    # one, which is the same blind spot that hid four defects behind the
    # hardcoded component page.
    lists = json.load(io.open(CHECKLISTS, encoding="utf-8"))
    names = {c["code"]: uk["checklists"][f"chk_{c['code']}"][0]
             for c in lists["checklists"]}
    out_lists = []
    for cl in lists["checklists"]:
        rows = []
        for it in cl["items"]:
            row = {"c": it["code"], "n": uk["checklists"][f"chk_{it['code']}"][0]}
            if it.get("node"):
                row["node"] = uk["components"][it["node"]][0]
            if it.get("logs"):
                row["logs"] = it["logs"]
            if it.get("appliesTo"):
                row["r"] = it["appliesTo"]
            if it.get("link"):
                row["link"] = it["link"]
            rows.append(row)
        out_lists.append({"c": cl["code"], "n": names[cl["code"]],
                          "season": cl["season"], "items": rows})

    blob = json.dumps({"items": items, "cats": cats, "lists": out_lists},
                      ensure_ascii=False, separators=(",", ":"))
    html = io.open(PROTO, encoding="utf-8").read()
    new, n = re.subn(r"const DATA = \{.*?\};",
                     lambda _: "const DATA = " + blob + ";", html, count=1,
                     flags=re.S)
    if n != 1:
        print("could not find the DATA blob in " + PROTO)
        return 1

    # Both figures on the first screen a reviewer reads, and both of them
    # agreeing with their own number. The previous version of this froze the
    # node count at 321 (it was inside the search pattern), froze the synonym
    # suffix at "-iv" (only the digits were replaced), and never touched the
    # screen-reader heading at all. See tools/uk_plural.py.
    total = sum(len(i["a"]) for i in items)
    NODE = ("вузол", "вузли", "вузлів")
    # "пошук по N ..." takes the locative, which is a different set of forms
    # from the plain count - one number, two grammatical positions.
    NODE_LOC = ("вузлу", "вузлах", "вузлах")
    SYN = ("синонім", "синоніми", "синонімів")

    subs = [
        (r"(Справжні дані: )\d+ вуз\w+( каталогу, українські назви, )\d+ синонім\w*",
         lambda mo: (mo.group(1) + phrase(len(items), *NODE) + mo.group(2)
                     + phrase(total, *SYN))),
        (r"(пошук по )\d+ вуз\w+",
         lambda mo: mo.group(1) + phrase(len(items), *NODE_LOC)),
    ]
    missed = []
    for pattern, repl in subs:
        new, hits = re.subn(pattern, repl, new, count=1)
        if not hits:
            missed.append(pattern)
    if missed:
        # Loudly: a silent miss is how 321 survived four catalog versions.
        print("could not update a count in " + PROTO + ":")
        for pattern in missed:
            print("    " + pattern)
        return 1

    io.open(PROTO, "w", encoding="utf-8", newline="\n").write(new)
    print(f"{PROTO}   {len(items)} components in {len(cats)} categories, "
          f"{total} aliases")
    return 0


if __name__ == "__main__":
    sys.exit(main())
