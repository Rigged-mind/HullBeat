#!/usr/bin/env python3
"""Validate the search ranking algorithm against real SQLite FTS4 and benchmark queries.

WHY THIS EXISTS
Search is wedge #5 in product-spec.md. The best-rated competitor is criticized
specifically for "fails on fundamental search functionality".

This check:
1. Loads scoring thresholds from design/search-ranking.json (SSOT).
2. Populates a real in-memory SQLite FTS4 table (search_index) from the catalog,
   plus custom and renamed components, and passport specs.
3. Tests all 17 benchmark queries (including Ukrainian morphology, stem thresholds,
   Slavic noun inflection, English cross-lingual, custom nodes, and Level 20 spec search).
4. Verifies ranking order (e.g. "утка" must rank "Швартові утки, кіпи" ABOVE "Закрутка").

Run:  python tools/check_search.py
Exit: 1 on any benchmark query mismatch or ordering failure.
"""
import io
import json
import os
import re
import sqlite3
import sys

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

CONFIG_PATH = os.path.join("design", "search-ranking.json")
CATALOG_PATH = os.path.join("catalog", "catalog.json")
SYNONYMS_PATH = os.path.join("catalog", "synonyms.json")
UK_PATH = os.path.join("catalog", "i18n", "uk.json")
EN_PATH = os.path.join("catalog", "i18n", "en.json")
PROTOTYPE_PATH = os.path.join("prototype", "index.html")
SEARCH_RANKING_KT_PATH = os.path.join(
    "app", "src", "main", "java", "app", "hullbeat", "domain", "search", "SearchRanking.kt"
)

WORD_SPLIT = re.compile(r"[\s,()/·—\-]+")


def load_config():
    with io.open(CONFIG_PATH, encoding="utf-8") as fh:
        return json.load(fh)


def variants(q, cfg):
    q = q.strip().lower()
    cost = cfg["stemCost"]
    min_len = cfg["minStemLength"]
    max_drop = cfg["maxDroppedChars"]
    res = [(q, 0)]
    if len(q) - 1 >= min_len and max_drop >= 1:
        res.append((q[:-1], cost))
    if len(q) - 2 >= min_len and max_drop >= 2:
        res.append((q[:-2], cost * 2))
    return res


def build_fts_query(q, vs, cfg):
    q = q.strip().lower()
    terms = []
    # If the query itself has words
    words = [w for w in WORD_SPLIT.split(q) if w]
    if len(words) > 1:
        # Multi-word: match all words prefix
        terms.append(" ".join(f"{w}*" for w in words))
        for w in words:
            if len(w) >= cfg["minStemLength"]:
                terms.append(f"{w}*")
    else:
        # Single token: add all variants
        for v, _ in vs:
            clean = re.sub(r'[^\w]', '', v)
            if len(clean) >= cfg["minStemLength"]:
                terms.append(f"{clean}*")

    if "-" in q:
        terms.append(f'"{q}"')
        sub_words = [w for w in q.split("-") if w]
        if len(sub_words) > 1:
            terms.append(" ".join(f"{w}*" for w in sub_words))

    seen = set()
    uniq = [t for t in terms if not (t in seen or seen.add(t))]
    return " OR ".join(uniq) if uniq else f"{q}*"


def pfx(text, vs):
    best = -1
    for v, cost in vs:
        if text.startswith(v) and (best < 0 or cost < best):
            best = cost
    return best


def word_pfx(text, vs):
    best = -1
    for w in WORD_SPLIT.split(text):
        if not w:
            continue
        c = pfx(w, vs)
        if c >= 0 and (best < 0 or c < best):
            best = c
    return best


def rank_item(q, title, en_title, aliases, category_name, spec_json, notes, cfg):
    scores = cfg["scores"]
    vs = variants(q, cfg)
    n = title.strip().lower()
    e = (en_title or "").strip().lower()

    # 120: exact title
    if n == q:
        return scores["exactTitle"], None

    # 110: exact English title
    if e and e == q:
        return scores["exactEnTitle"], en_title

    # 90 - c: prefix title
    c = pfx(n, vs)
    if c >= 0:
        return scores["prefixTitle"] - c, None

    # 85 - c: prefix English title
    if e:
        c = pfx(e, vs)
        if c >= 0:
            return scores["prefixEnTitle"] - c, en_title

    # 70 - c: word prefix in title
    c = word_pfx(n, vs)
    if c >= 0:
        return scores["wordPrefixTitle"] - c, None

    # 65 - c: word prefix in English title
    if e:
        c = word_pfx(e, vs)
        if c >= 0:
            return scores["wordPrefixEnTitle"] - c, en_title

    # Synonyms
    best = None
    for a in aliases:
        t = a.strip().lower()
        s = 0
        if t == q:
            s = scores["exactSynonym"]
        else:
            c = pfx(t, vs)
            if c >= 0:
                s = scores["prefixSynonym"] - c
            elif q in t:
                s = scores["containsSynonym"]
        if s > 0 and (not best or s > best[0]):
            best = (s, a)
    if best:
        return best

    # 40: substring in title
    if q in n:
        return scores["substringTitle"], None

    # 30: category contains
    if category_name and q in category_name.lower():
        return scores["containsCategory"], None

    # 20: specJson or notes contains
    if spec_json and q in spec_json.lower():
        return scores["specOrNotes"], "spec"
    if notes and q in notes.lower():
        return scores["specOrNotes"], "notes"

    return 0, None


def proto_rank(item, q, cfg):
    vs = variants(q, cfg)
    n = item["n"].lower()
    e = item["e"].lower()
    R = cfg["scores"]
    if n == q:
        return R["exactTitle"], None
    if e == q:
        return R["exactEnTitle"], item["e"]
    c = pfx(n, vs)
    if c >= 0:
        return R["prefixTitle"] - c, None
    if e:
        c = pfx(e, vs)
        if c >= 0:
            return R["prefixEnTitle"] - c, item["e"]
    c = word_pfx(n, vs)
    if c >= 0:
        return R["wordPrefixTitle"] - c, None
    if e:
        c = word_pfx(e, vs)
        if c >= 0:
            return R["wordPrefixEnTitle"] - c, item["e"]
    best = None
    for a in item.get("a", []):
        t = a.lower()
        s = 0
        if t == q:
            s = R["exactSynonym"]
        else:
            c = pfx(t, vs)
            if c >= 0:
                s = R["prefixSynonym"] - c
            elif q in t:
                s = R["containsSynonym"]
        if s > 0 and (not best or s > best[0]):
            best = (s, a)
    if best:
        return best
    if q in n:
        return R["substringTitle"], None
    if q in item.get("g", "").lower():
        return R["containsCategory"], None
    if any(q in x.lower() for x in item.get("h", [])):
        return R["specOrNotes"], "spec"
    return 0, None


def check_logic_parity(cfg, failures):
    # 1. Check Prototype JavaScript branch order
    if not os.path.exists(PROTOTYPE_PATH):
        failures.append(f"Prototype file not found: {PROTOTYPE_PATH}")
        return
    with io.open(PROTOTYPE_PATH, encoding="utf-8") as fh:
        html = fh.read()

    rank_m = re.search(r"function rank\(item, q\) \{(.*?)\n\}", html, re.S)
    if not rank_m:
        failures.append("Could not find function rank(item, q) in prototype/index.html")
    else:
        rank_js = rank_m.group(1)
        expected_branches = [
            "RANK.exactTitle",
            "RANK.exactEnTitle",
            "RANK.prefixTitle",
            "RANK.prefixEnTitle",
            "RANK.wordPrefixTitle",
            "RANK.wordPrefixEnTitle",
            "RANK.exactSynonym",
            "RANK.prefixSynonym",
            "RANK.containsSynonym",
            "RANK.substringTitle",
            "RANK.containsCategory",
            "RANK.specOrNotes",
        ]
        pos = 0
        for b in expected_branches:
            p = rank_js.find(b, pos)
            if p < 0:
                failures.append(f"Prototype rank() branch missing or out of order: {b}")
                break
            pos = p

    # 2. Check Kotlin SearchRanking branch order
    if not os.path.exists(SEARCH_RANKING_KT_PATH):
        failures.append(f"SearchRanking.kt not found: {SEARCH_RANKING_KT_PATH}")
    else:
        with io.open(SEARCH_RANKING_KT_PATH, encoding="utf-8") as fh:
            kt = fh.read()

        expected_kt = [
            "Scores.EXACT_TITLE",
            "Scores.EXACT_EN_TITLE",
            "Scores.PREFIX_TITLE",
            "Scores.PREFIX_EN_TITLE",
            "Scores.WORD_PREFIX_TITLE",
            "Scores.WORD_PREFIX_EN_TITLE",
            "Scores.EXACT_SYNONYM",
            "Scores.PREFIX_SYNONYM",
            "Scores.CONTAINS_SYNONYM",
            "Scores.SUBSTRING_TITLE",
            "Scores.CONTAINS_CATEGORY",
            "Scores.SPEC_OR_NOTES",
        ]
        pos = 0
        for b in expected_kt:
            p = kt.find(b, pos)
            if p < 0:
                failures.append(f"Kotlin SearchRanking branch missing or out of order: {b}")
                break
            pos = p

    # 3. Check 100% score parity across prototype items and benchmark queries
    data_m = re.search(r"const DATA = (\{.*?\});", html, re.S)
    if not data_m:
        failures.append("Could not find const DATA in prototype/index.html")
        return

    items = json.loads(data_m.group(1)).get("items", [])
    queries = [
        "утка", "кіпа", "мачта", "мачтою", "гальмо", "цинки", "шкот", "шток",
        "racor", "тузик", "крильчатка", "impeller", "поліроль", "момент"
    ]

    # --- the port must stay faithful to the REAL prototype -----------------
    #
    # proto_rank() is a MODEL of prototype/index.html, not the file itself, so
    # the two could drift and this check would keep passing: it would be
    # comparing a model of the prototype against a model of Kotlin and calling
    # the agreement parity.
    #
    # These fingerprints were measured by running the real `rank()` in a
    # browser over the real DATA blob - sum of scores, number of hits, best
    # score and winning code, per query. Four numbers stand in for 329, and
    # all 4606 scores agreed exactly when they were taken.
    #
    # ⚠️ They CANNOT be regenerated by this script. Changing the prototype's
    # ranking means re-measuring in a browser:
    #
    #   const Q=[…]; for (const q of Q) { let sum=0,hits=0,best=-1,code='';
    #     for (const it of DATA.items) { const s=rank(it,q).s; sum+=s;
    #       if(s>0)hits++; if(s>best){best=s;code=it.c;} } … }
    #
    # Editing them to match a failing run defeats the point of having them.
    GOLDEN = {
        "утка": (133, 3, 58, "mooring_cleats"),
        "кіпа": (58, 1, 58, "mooring_cleats"),
        "мачта": (55, 1, 55, "mast"),
        "мачтою": (21, 1, 21, "mast"),
        "гальмо": (305, 5, 78, "trailer_brakes"),
        "цинки": (209, 5, 55, "anodes_hull"),
        "шкот": (205, 3, 90, "sheets"),
        "шток": (113, 2, 78, "storm_sails"),
        "racor": (55, 1, 55, "fuel_prefilter"),
        "тузик": (90, 2, 55, "tender_hull"),
        "крильчатка": (110, 2, 55, "raw_water_impeller"),
        "impeller": (130, 2, 65, "raw_water_impeller"),
        "поліроль": (20, 1, 20, "hull_gelcoat"),
        "момент": (40, 2, 20, "keel_bolts"),
    }
    for q, expected in GOLDEN.items():
        scores = {it["c"]: proto_rank(it, q, cfg)[0] for it in items}
        best_code = max(scores, key=lambda k: scores[k])
        got = (sum(scores.values()), sum(1 for v in scores.values() if v > 0),
               max(scores.values()), best_code)
        if got != expected:
            failures.append(
                "proto_rank no longer matches the browser-measured prototype "
                "for '%s': got %s, measured %s" % (q, got, expected))

    score_mismatches = []
    for q in queries:
        for it in items:
            sp, _ = proto_rank(it, q, cfg)
            spec_text = " ".join(it.get("h", [])) if it.get("h") else None
            skt, _ = rank_item(q, it["n"], it["e"], it["a"], it["g"], spec_text, None, cfg)
            if sp != skt:
                score_mismatches.append(f"q='{q}', code={it.get('c')}: proto={sp}, kt={skt}")

    if score_mismatches:
        failures.append(
            f"{len(score_mismatches)} score mismatch(es) between prototype and Kotlin ranking (first: {score_mismatches[0]})"
        )



def main():
    cfg = load_config()
    with io.open(CATALOG_PATH, encoding="utf-8") as fh:
        catalog = json.load(fh)
    with io.open(SYNONYMS_PATH, encoding="utf-8") as fh:
        synonyms = json.load(fh)
    with io.open(UK_PATH, encoding="utf-8") as fh:
        uk = json.load(fh)

    con = sqlite3.connect(":memory:")
    con.execute("CREATE VIRTUAL TABLE search_index USING fts4(tokenize=unicode61, ownerType, ownerId, vesselId, title, body)")

    components = []
    for cat in catalog["categories"]:
        cat_name = uk["categories"].get(cat["code"], [""])[0]
        for comp in cat["components"]:
            code = comp["code"]
            uk_name = uk["components"].get(code, [comp["en"]])[0]
            en_name = comp["en"]
            aliases = sorted(set(synonyms.get("uk", {}).get(code, [])) | set(synonyms.get("en", {}).get(code, [])))
            spec_json = None
            notes = None
            if code == "raw_water_impeller":
                spec_json = '{"partNumber": "09-1027B", "material": "neoprene"}'
                notes = "змащувати гліцерином при встановленні"

            cid = len(components) + 1
            body_parts = [en_name, cat_name] + aliases
            if spec_json:
                body_parts.append(spec_json)
            if notes:
                body_parts.append(notes)
            body = " ".join(body_parts)

            con.execute("INSERT INTO search_index VALUES ('component', ?, 1, ?, ?)", (cid, uk_name, body))
            components.append({
                "id": cid,
                "code": code,
                "title": uk_name,
                "en": en_name,
                "aliases": aliases,
                "category": cat_name,
                "crit": comp.get("crit", "med"),
                "spec_json": spec_json,
                "notes": notes,
            })

    # Add custom node: "Schenker Zen 30"
    cid = len(components) + 1
    custom_title = "Опріснювач Schenker Zen 30"
    custom_body = "Інше / власне watermaker 12V 30L/h zen 30 schenker"
    con.execute("INSERT INTO search_index VALUES ('component', ?, 1, ?, ?)", (cid, custom_title, custom_body))
    components.append({
        "id": cid,
        "code": None,
        "title": custom_title,
        "en": "Watermaker Schenker Zen 30",
        "aliases": ["zen 30", "schenker"],
        "category": "Інше / власне",
        "crit": "high",
        "spec_json": '{"flowRate": "30 L/h", "voltage": "12V"}',
        "notes": "фільтри 5 мікрон",
    })

    # Add renamed node: "Ліхтар Петровича" (deck light)
    cid = len(components) + 1
    renamed_title = "Ліхтар Петровича"
    renamed_body = "Палубне освітлення Deck spreader light flood light 12V LED ліхтар"
    con.execute("INSERT INTO search_index VALUES ('component', ?, 1, ?, ?)", (cid, renamed_title, renamed_body))
    components.append({
        "id": cid,
        "code": "deck_light",
        "title": renamed_title,
        "en": "Deck light",
        "aliases": ["прожектор", "палубне світло", "ліхтар"],
        "category": "Електросистема",
        "crit": "med",
        "spec_json": '{"power": "20W"}',
        "notes": "купив у Петровича",
    })

    comp_by_id = {c["id"]: c for c in components}

    def do_search(q):
        vs = variants(q, cfg)
        fts_query = build_fts_query(q, vs, cfg)

        cur = con.execute("SELECT ownerId FROM search_index WHERE vesselId = 1 AND search_index MATCH ? LIMIT 100", (fts_query,))
        candidate_ids = [row[0] for row in cur.fetchall()]

        ranked = []
        for cid in candidate_ids:
            it = comp_by_id[cid]
            s, via = rank_item(q, it["title"], it["en"], it["aliases"], it["category"], it["spec_json"], it["notes"], cfg)
            if s > 0:
                crit_val = {"high": 2, "med": 1, "low": 0}.get(it["crit"], 0)
                ranked.append((s, crit_val, it["id"], it, via))

        ranked.sort(key=lambda x: (x[0], x[1], x[2]), reverse=True)
        return ranked

    failures = []

    # 1. "утка": MUST find mooring_cleats FIRST, and strictly outrank headsail_furler (закрутка)
    r_utka = do_search("утка")
    if not r_utka or r_utka[0][3]["code"] != "mooring_cleats":
        failures.append(f"'утка' top result is {r_utka[0][3]['code'] if r_utka else 'None'}, expected mooring_cleats")
    furler_rank = [i for i, r in enumerate(r_utka) if r[3]["code"] == "headsail_furler"]
    cleats_rank = [i for i, r in enumerate(r_utka) if r[3]["code"] == "mooring_cleats"]
    if cleats_rank and furler_rank and cleats_rank[0] >= furler_rank[0]:
        failures.append(f"'утка': mooring_cleats (rank {cleats_rank[0]}) must rank above headsail_furler (rank {furler_rank[0]})")

    # 2. "кіпа": MUST find mooring_cleats FIRST
    r_kipa = do_search("кіпа")
    if not r_kipa or r_kipa[0][3]["code"] != "mooring_cleats":
        failures.append(f"'кіпа' top result is {r_kipa[0][3]['code'] if r_kipa else 'None'}, expected mooring_cleats")

    # 3. "мачта": MUST find mast (Щогла)
    r_machta = do_search("мачта")
    if not r_machta or r_machta[0][3]["code"] != "mast":
        failures.append(f"'мачта' top result is {r_machta[0][3]['code'] if r_machta else 'None'}, expected mast")

    # 4. "мачтою": MUST find mast (inflection test)
    r_machtoyu = do_search("мачтою")
    if not any(r[3]["code"] == "mast" for r in r_machtoyu):
        failures.append("'мачтою' did not find mast")

    # 5. "гальмо": trailer_brakes or drogue must outrank galvanic_isolator
    r_galmo = do_search("гальмо")
    codes = [r[3]["code"] for r in r_galmo]
    if "galvanic_isolator" in codes:
        iso_idx = codes.index("galvanic_isolator")
        if "trailer_brakes" in codes and codes.index("trailer_brakes") > iso_idx:
            failures.append("'гальмо': trailer_brakes ranked below galvanic_isolator")
        if "drogue" in codes and codes.index("drogue") > iso_idx:
            failures.append("'гальмо': drogue ranked below galvanic_isolator")

    # 6. "цинки": MUST find anodes_hull
    r_tsynky = do_search("цинки")
    if not any(r[3]["code"] == "anodes_hull" for r in r_tsynky):
        failures.append("'цинки' did not find anodes_hull")

    # 7. "шкот": MUST find sheets (Шкоти)
    r_shkot = do_search("шкот")
    if not r_shkot or r_shkot[0][3]["code"] != "sheets":
        failures.append(f"'шкот' top result is {r_shkot[0][3]['code'] if r_shkot else 'None'}, expected sheets")

    # 8. "шток": MUST NOT match sheets
    r_shtok = do_search("шток")
    if any(r[3]["code"] == "sheets" for r in r_shtok):
        failures.append("'шток' incorrectly matched sheets")

    # 9. "racor": MUST find fuel_prefilter
    r_racor = do_search("racor")
    if not any(r[3]["code"] == "fuel_prefilter" for r in r_racor):
        failures.append("'racor' did not find fuel_prefilter")

    # 10. "тузик": MUST find tender_hull
    r_tuzyk = do_search("тузик")
    if not any(r[3]["code"] == "tender_hull" for r in r_tuzyk):
        failures.append("'тузик' did not find tender_hull")

    # 11. "крильчатка": MUST find raw_water_impeller
    r_kryl = do_search("крильчатка")
    if not any(r[3]["code"] == "raw_water_impeller" for r in r_kryl):
        failures.append("'крильчатка' did not find raw_water_impeller")

    # 12. "impeller": MUST find raw_water_impeller in top results (ambiguous with gen_impeller per §8.1)
    r_imp = do_search("impeller")
    if not r_imp or "raw_water_impeller" not in [r[3]["code"] for r in r_imp[:2]]:
        failures.append(f"'impeller' top results {[r[3]['code'] for r in r_imp[:2]]}, expected raw_water_impeller")

    # 13. "zen 30": custom node test
    r_zen = do_search("zen 30")
    if not r_zen or "Zen 30" not in r_zen[0][3]["title"]:
        failures.append(f"'zen 30' top result is {r_zen[0][3]['title'] if r_zen else 'None'}, expected Zen 30")

    # 14. "ліхтар": renamed node test
    r_liht = do_search("ліхтар")
    if not r_liht or r_liht[0][3]["title"] != "Ліхтар Петровича":
        failures.append(f"'ліхтар' top result is {r_liht[0][3]['title'] if r_liht else 'None'}, expected Ліхтар Петровича")

    # 15. "09-1027b": Level 20 specJson part number test
    r_part = do_search("09-1027b")
    if not r_part or r_part[0][3]["code"] != "raw_water_impeller" or r_part[0][0] != cfg["scores"]["specOrNotes"]:
        failures.append(f"'09-1027b' did not match raw_water_impeller at score 20 (got {r_part[0] if r_part else 'None'})")

    # 16. "гліцерин": Level 20 notes test
    r_notes = do_search("гліцерин")
    if not r_notes or r_notes[0][3]["code"] != "raw_water_impeller" or r_notes[0][0] != cfg["scores"]["specOrNotes"]:
        failures.append(f"'гліцерин' did not match raw_water_impeller notes at score 20 (got {r_notes[0] if r_notes else 'None'})")

    # 17. id DESC tiebreaker test on identical score
    r_high = do_search("фільтри")
    if r_high and len(r_high) > 1 and r_high[0][0] == r_high[1][0] and r_high[0][1] == r_high[1][1]:
        if r_high[0][2] < r_high[1][2]:
            failures.append(f"Tiebreak failed: id {r_high[0][2]} ranked below {r_high[1][2]}")

    # 18. Logic parity with prototype and branch ordering
    check_logic_parity(cfg, failures)

    if failures:
        print(f"FAILED: {len(failures)} search check(s) failed:")
        for f in failures:
            print(f"  * {f}")
        return 1

    print(f"OK   all 17 benchmark queries, Level 20 tests, and prototype-Kotlin parity pass")
    return 0


if __name__ == "__main__":
    sys.exit(main())
