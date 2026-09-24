#!/usr/bin/env python3
"""Validate catalog/catalog.json.

Checks structure, uniqueness, cross-references and applicability vocabulary.
Exit code 0 = valid, 1 = errors found. Warnings do not fail the build.

Usage:  python tools/validate_catalog.py [path/to/catalog.json]
"""
import json
import sys
import os
import re

CRIT = {"high", "med", "low"}
# Every key a component may carry. Without this a typo - `leadDay`, `expire`
# - is simply ignored, and the value it was meant to set stays at its
# default for as long as nobody looks.
COMPONENT_KEYS = {
    "code", "en", "crit", "sched", "verify", "perEngine", "expires",
    "leadDays", "reference", "parent", "specHints", "appliesTo", "icon",
}

SCHED_KEYS = {"days", "hours", "miles"}
APPLIES_KEYS = {
    "hullAny": "hull",
    "engineAny": "engine",
    "driveAny": "drive",
    "coolingAny": "cooling",
    "rigAny": "rig",
    "keelAny": "keel",
    "extrasAny": "extras",
    "storageAny": "storage",
}
CODE_RE = re.compile(r"^[a-z][a-z0-9_]*$")

errors = []
warnings = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def main(path):
    if not os.path.exists(path):
        print(f"FATAL: not found: {path}")
        return 1
    try:
        with open(path, encoding="utf-8") as fh:
            cat = json.load(fh)
    except json.JSONDecodeError as e:
        print(f"FATAL: invalid JSON at line {e.lineno} col {e.colno}: {e.msg}")
        return 1

    vocab = cat.get("profileVocabulary")
    if not isinstance(vocab, dict):
        print("FATAL: profileVocabulary missing")
        return 1
    vocab_sets = {k: set(v) for k, v in vocab.items()}

    if not isinstance(cat.get("version"), int):
        err("version must be an integer")

    cat_codes = set()
    comp_codes = {}          # code -> category code
    n_comp = 0
    n_verify = 0
    n_sched = 0

    categories = cat.get("categories")
    if not isinstance(categories, list) or not categories:
        print("FATAL: categories missing or empty")
        return 1

    sorts = []

    for ci, c in enumerate(categories):
        where = f"category[{ci}]"
        code = c.get("code")
        if not code or not CODE_RE.match(str(code)):
            err(f"{where}: bad or missing code {code!r}")
            continue
        where = f"category '{code}'"
        if code in cat_codes:
            err(f"{where}: duplicate category code")
        cat_codes.add(code)

        for field in ("en", "icon"):
            if not c.get(field):
                err(f"{where}: missing '{field}'")
        if not isinstance(c.get("sort"), int):
            err(f"{where}: 'sort' must be an integer")
        else:
            sorts.append(c["sort"])

        check_applies(c.get("appliesTo"), vocab_sets, where)

        comps = c.get("components")
        if not isinstance(comps, list):
            err(f"{where}: 'components' must be a list")
            continue

        local_codes = set()
        parents_needed = []

        for k, comp in enumerate(comps):
            cw = f"{where} / component[{k}]"
            ccode = comp.get("code")
            if not ccode or not CODE_RE.match(str(ccode)):
                err(f"{cw}: bad or missing code {ccode!r}")
                continue
            cw = f"{where} / '{ccode}'"
            n_comp += 1

            if ccode in comp_codes:
                err(f"{cw}: duplicate component code (also in '{comp_codes[ccode]}')")
            comp_codes[ccode] = code
            local_codes.add(ccode)

            if not comp.get("en"):
                err(f"{cw}: missing 'en'")
            crit = comp.get("crit")
            if crit not in CRIT:
                err(f"{cw}: crit must be one of {sorted(CRIT)}, got {crit!r}")

            lead = comp.get("leadDays")
            if lead is not None and (not isinstance(lead, int) or not 1 <= lead <= 365):
                err(f"{cw}: 'leadDays' must be 1..365 days, got {lead!r}")
            # A lead time is only load-bearing where there is NO interval.
            # With a `sched` the node already turns amber on its own window;
            # without one the date printed on the paper is all there is, and
            # the lead time is the only thing that warns before it passes.
            if comp.get("expires") and comp.get("sched") is None and lead is None:
                err(f"{cw}: expires with no interval and no 'leadDays' - "
                    f"nothing would warn before the date on it passes")
            for key in comp:
                if key not in COMPONENT_KEYS:
                    err(f"{cw}: unknown key {key!r}")

            sched = comp.get("sched")
            if sched is not None:
                if not isinstance(sched, dict) or not sched:
                    err(f"{cw}: 'sched' must be a non-empty object")
                else:
                    n_sched += 1
                    extra = set(sched) - SCHED_KEYS
                    if extra:
                        err(f"{cw}: unknown sched keys {sorted(extra)}")
                    for sk, sv in sched.items():
                        if not isinstance(sv, int) or sv <= 0:
                            err(f"{cw}: sched.{sk} must be a positive integer, got {sv!r}")

            if "perEngine" in comp:
                if not isinstance(comp["perEngine"], bool):
                    err(f"{cw}: 'perEngine' must be a boolean")
                elif comp["perEngine"] is True:
                    warn(f"{cw}: 'perEngine: true' is redundant - the category decides")
                elif not c.get("perEngine"):
                    err(f"{cw}: 'perEngine: false' is meaningless outside a "
                        f"perEngine category")

            if comp.get("verify"):
                n_verify += 1
                if sched is None:
                    warn(f"{cw}: verify=true but no default sched to verify")

            if comp.get("reference"):
                if sched is not None or comp.get("expires"):
                    err(f"{cw}: 'reference' cannot be combined with 'sched' or 'expires'")
            elif not comp.get("expires") and sched is None:
                warn(f"{cw}: no 'sched', not 'expires', not 'reference' - will never become due")

            hints = comp.get("specHints")
            if hints is not None and (not isinstance(hints, list) or
                                      not all(isinstance(h, str) and h for h in hints)):
                err(f"{cw}: 'specHints' must be a list of non-empty strings")

            check_applies(comp.get("appliesTo"), vocab_sets, cw)

            if comp.get("parent"):
                parents_needed.append((comp["parent"], cw))

        for parent, cw in parents_needed:
            if parent not in local_codes:
                err(f"{cw}: parent '{parent}' not found in the same category")

    if len(set(sorts)) != len(sorts):
        err("duplicate 'sort' values across categories")

    # Report
    print(f"catalog v{cat.get('version')}  ({cat.get('generated')})")
    print(f"  categories : {len(cat_codes)}")
    print(f"  components : {n_comp}")
    print(f"  with sched : {n_sched}")
    print(f"  verify=true: {n_verify}  (must be checked against manufacturer manuals)")
    print()

    check_synonym_values(cat)
    check_parent_gates(cat)

    for w in warnings:
        print(f"WARN  {w}")
    if warnings:
        print(f"      ({len(warnings)} warnings)")
        print()
    for e in errors:
        print(f"ERROR {e}")

    if errors:
        print(f"\nFAILED: {len(errors)} error(s)")
        return 1
    print("OK")
    return 0


def check_parent_gates(cat):
    """A child must be gated at least as tightly as its parent.

    The tree screen made this visible: a 3 m inflatable showed "BMS і
    балансування літію" with no "Сервісні акумулятори" above it, and
    "Перевертання якірного ланцюга" with no "Якірний ланцюг". The children
    carried no rule, the parents were gated on hull, and a node with no rule
    applies to every boat - so each child outlived its own parent.

    The check is structural, not persona-based, and that matters: it also
    caught `stove_gimbal`, whose parent `stove` needs a cabin. No reference
    boat exposed that one, because `stove_gimbal` has a gas rule of its own
    that happens to exclude every cabin-less persona we model. A boat with LPG
    and no cabin would have shown a gimbal with nothing mounted in it.

    Sufficient condition: for every key in the parent's effective rule, the
    child must carry that key with a subset of the parent's values. A child
    may be gated MORE (extra keys, narrower values); it may never be gated
    less.
    """
    comp, incat = {}, {}
    for c in cat.get("categories", []):
        for node in c.get("components", []):
            comp[node["code"]] = node
            incat[node["code"]] = c

    for code, node in sorted(comp.items()):
        par = node.get("parent")
        if not par or par not in comp:
            continue
        # A child in the same category already inherits that category's rule,
        # so only fold in the parent's category rule when they differ.
        prule = dict(comp[par].get("appliesTo") or {})
        if incat[par]["code"] != incat[code]["code"]:
            prule.update(incat[par].get("appliesTo") or {})
        crule = node.get("appliesTo") or {}
        for key, pvals in prule.items():
            cvals = crule.get(key)
            if cvals is None:
                err(f"{code}: parent {par} is gated on {key}={pvals}, "
                    f"but the child carries no {key} - it would outlive its parent")
            elif not set(cvals) <= set(pvals):
                extra = sorted(set(cvals) - set(pvals))
                err(f"{code}: {key}={extra} is admitted by the child but not "
                    f"by its parent {par} ({pvals})")


def check_synonym_values(cat):
    """Flag a profile value that never appears in a rule on its own.

    This is the signature of the `shaft` bug. `propulsion` offered both
    `inboard_diesel` and `shaft`, which mean the same installation, and all ten
    rules that mentioned `shaft` listed it defensively beside `inboard_diesel`.
    `engine_main` was the one rule that forgot the hedge, so a boat recorded as
    `shaft` came back with no engine at all - no oil, no impeller, no filters.

    Existing checks could not see it: every rule referenced a value the
    vocabulary defined, and every value was referenced by some rule. What gave
    it away is that `shaft` never once appeared WITHOUT `inboard_diesel`. A
    value that cannot stand alone is a synonym of whatever it hides behind, and
    a synonym in a single-select dimension is a value some rule will forget.
    """
    rules = {}
    for c in cat.get("categories", []):
        for node in [c] + c.get("components", []):
            for key, values in (node.get("appliesTo") or {}).items():
                if not isinstance(values, list):
                    continue
                rules.setdefault(key, []).append(set(values))

    for key, sets in rules.items():
        seen = set().union(*sets) if sets else set()
        for value in sorted(seen):
            mentions = [s for s in sets if value in s]
            if len(mentions) < 3:
                continue        # too few rules to draw a conclusion from
            always_with = set.intersection(*mentions) - {value}
            if not always_with:
                continue
            # A genuine group is MUTUAL: the three sail hulls never appear
            # apart because a mast applies to all three equally, and that is
            # correct, not a bug. A forgotten duplicate is ONE-SIDED - `shaft`
            # never appeared without `inboard_diesel`, but `inboard_diesel`
            # appeared plenty of times without `shaft`, and every one of those
            # was a rule the duplicate silently fell out of.
            one_sided = {
                other for other in always_with
                if any(other in s and value not in s for s in sets)
            }
            if one_sided:
                warn(f"{key}: '{value}' never appears without "
                     f"{sorted(one_sided)} across {len(mentions)} rules, but "
                     f"{sorted(one_sided)} appears without it - likely a "
                     "duplicate value that some rule has already forgotten")


def check_applies(applies, vocab_sets, where):
    if applies is None:
        return
    if not isinstance(applies, dict):
        err(f"{where}: 'appliesTo' must be an object")
        return
    for key, values in applies.items():
        if key == "enginesMin":
            if not isinstance(values, int) or values < 1:
                err(f"{where}: enginesMin must be a positive integer")
            continue
        vocab_key = APPLIES_KEYS.get(key)
        if vocab_key is None:
            err(f"{where}: unknown appliesTo key '{key}' "
                f"(allowed: {sorted(list(APPLIES_KEYS) + ['enginesMin'])})")
            continue
        if not isinstance(values, list) or not values:
            err(f"{where}: appliesTo.{key} must be a non-empty list")
            continue
        allowed = vocab_sets.get(vocab_key, set())
        bad = [v for v in values if v not in allowed]
        if bad:
            err(f"{where}: appliesTo.{key} has values outside vocabulary: {bad}")


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join("catalog", "catalog.json")
    sys.exit(main(target))
