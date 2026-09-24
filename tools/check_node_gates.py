"""A checklist item must not outlive the node it points at.

Every checklist item names a `node` - the catalog component it exercises -
and the review file prints that node's approved term beside it. If the item's
rule is wider than the node's, the app gives an order about a part the boat
does not own: the line is there, the node is not, and tapping through goes
nowhere.

This was found twice by hand before it was written down:

  * `sf_drogue` / `sf_sea_anchor` carried no rule at all while the nodes
    `drogue` and `sea_anchor` sat behind `extrasAny: ["offshore"]`, so a 3 m
    tender was told to lay out a parachute anchor.
  * `wz_raw_antifreeze` fired on any inboard while `raw_water_hoses` was
    gated on the hull.

Twice is a class, not a coincidence.

WHAT "WIDER" MEANS
------------------
The effective rule of a node is the CATEGORY's rule AND the node's own -
`Catalog.seedFor` walks matching categories and then filters components
inside them, so both must pass. Composing them is not one operation, though,
because the axes are not one kind:

  * hull, engine, drive, cooling, rig, keel, storage hold ONE value per
    vessel, so two rules on the same axis intersect, and the item must
    constrain that axis to a subset of the intersection.

  * `extras` is a SET. `extrasAny` asks "does the boat have any of these",
    so a category asking for [heater, aircon] and a node asking for [cabin]
    is perfectly satisfiable - by a boat with a cabin and a heater. The two
    stay as two requirements, and the item has to imply each of them.

Getting that wrong in the first draft turned 6 findings into 22.

CROSS-AXIS IMPLICATION
----------------------
The axes are not independent. An item gated on `driveAny: [shaft, ...]`
already implies the boat has an engine, so it need not repeat
`engineAny: [diesel, petrol]`. Without this, sixteen correct items are
reported. Implications are declared here, explicitly, rather than inferred.
"""

import io
import json
import os
import sys

CATALOG = os.path.join("catalog", "catalog.json")
CHECKLISTS = os.path.join("catalog", "checklists.json")

SINGLE = ("hullAny", "engineAny", "driveAny", "coolingAny",
          "rigAny", "keelAny", "storageAny")

# "an item constrained like this also satisfies that" - the only kind of
# knowledge a subset test cannot derive from the files.
IMPLIES = [
    # A boat with a drive has an engine.
    (("driveAny", {"shaft", "saildrive", "sterndrive", "outboard"}),
     ("engineAny", {"diesel", "petrol"})),
    # Raw-water cooling is a property of an engine's circuit.
    (("coolingAny", {"raw_water", "keel_cooled", "air"}),
     ("engineAny", {"diesel", "petrol"})),
]

# Pairs that are known to be wider and are waiting on a decision that is not
# this script's to make. Each one is a real finding, kept visible and counted
# rather than quietly passing. Anything NOT on this list fails the build.
KNOWN_OPEN = {
    # Empty, and meant to stay that way. Every entry that was here got closed
    # by moving the rule to whichever side was actually wrong:
    #
    #   pd_fuel            item had no rule at all -> engineAny [diesel, petrol]
    #   wz_furler          item -> + hullAny SAIL, which its node inherits
    #   wz_raw_antifreeze  item -> + hullAny DECKED, which its node keeps
    #                      through its parent `through_hulls`
    #   wz/cm_water_heater the CATEGORY was wrong: a calorifier is fresh-water
    #                      plumbing heated off the engine, not AC electrics
    #   wz_vent            the CATEGORY was wrong: condensation does not wait
    #                      for a heater to be fitted
    #
    # Adding an entry here is allowed, and it is a debt: say who has to decide
    # and why. The step fails on anything not listed, and fails again when a
    # listed pair stops being wider, so the list cannot rot in either
    # direction.
}


def implied(item_rule, axis, allowed):
    for (src_axis, src_vals), (dst_axis, dst_vals) in IMPLIES:
        if dst_axis != axis or not dst_vals <= set(allowed):
            continue
        got = item_rule.get(src_axis)
        if got and set(got) <= src_vals:
            return True
    return False


def main():
    catalog = json.load(io.open(CATALOG, encoding="utf-8"))
    # effective requirement of a node: single axes intersected, extras kept
    # as a list of separate any-of clauses.
    single, extras = {}, {}
    for cat in catalog["categories"]:
        crule = cat.get("appliesTo") or {}
        for comp in cat["components"]:
            nrule = comp.get("appliesTo") or {}
            axes = {}
            for key in SINGLE:
                sets = [set(r[key]) for r in (crule, nrule) if key in r]
                if sets:
                    axes[key] = set.intersection(*sets)
            single[comp["code"]] = axes
            extras[comp["code"]] = [set(r["extrasAny"])
                                    for r in (crule, nrule) if "extrasAny" in r]

    data = json.load(io.open(CHECKLISTS, encoding="utf-8"))
    linked = failures = open_known = 0
    stale = set(KNOWN_OPEN)

    for cl in data["checklists"]:
        for item in cl["items"]:
            node = item.get("node")
            if not node or node not in single:
                continue
            linked += 1
            rule = item.get("appliesTo") or {}
            wider = []

            for axis, allowed in single[node].items():
                got = rule.get(axis)
                if got is not None and set(got) <= allowed:
                    continue
                if implied(rule, axis, allowed):
                    continue
                wider.append("%s: item %s, node %s"
                             % (axis, sorted(got) if got else "unconstrained",
                                sorted(allowed)))

            for clause in extras[node]:
                got = rule.get("extrasAny")
                if got is None or not set(got) <= clause:
                    wider.append("extrasAny: item %s, node %s"
                                 % (sorted(got) if got else "unconstrained",
                                    sorted(clause)))

            if not wider:
                if item["code"] in KNOWN_OPEN:
                    # Report it once, as fixed - not a second time as stale.
                    stale.discard(item["code"])
                    print("    NO LONGER WIDER, drop it from KNOWN_OPEN: %s"
                          % item["code"])
                    failures += 1
                continue
            if item["code"] in KNOWN_OPEN:
                stale.discard(item["code"])
                open_known += 1
                continue
            failures += 1
            print("    WIDER THAN ITS NODE  %s -> %s" % (item["code"], node))
            for line in wider:
                print("        %s" % line)

    for code in sorted(stale):
        print("    KNOWN_OPEN names an item that no longer exists: %s" % code)
        failures += 1

    print("checklist items linked to a node    %3d checked, %d wider than "
          "their node (%d known, awaiting a decision)"
          % (linked, failures + open_known, open_known))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
