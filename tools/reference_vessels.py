#!/usr/bin/env python3
"""Expand the catalog for real reference vessels and assert the cardinalities.

Three bugs this session were the same shape: a rule that existed and was
syntactically valid, but was not applied everywhere it should be.

  * `water` was in profileVocabulary and in the anode specHints - not in Vessel
  * `emergency_tiller` had a 365-day interval - and no checklist item anywhere
  * `perEngine` was on engine_main - and not on drivetrain

`validate_catalog.py` cannot catch any of them. It checks referential
integrity: does the code exist, is the type right, is there a translation. A
missing flag is a legal state - the field is optional and defaults to false.

So this checks a different thing. It builds the component tree for several real
boats and asserts what must be physically true of each: a catamaran with two
saildrives has two saildrive anodes and one sea chest; a trailered outboard
has no shaft at all but must have a transom drain plug. The question moves
from "is the JSON well formed" to "does this describe a boat".

Run:  python tools/reference_vessels.py
Exit: 1 on any failed assertion.
"""
import io
import json
import os
import sys
from collections import Counter

CATALOG = os.path.join("catalog", "catalog.json")
CHECKLISTS = os.path.join("catalog", "checklists.json")

RULE_FIELD = {
    "hullAny": "hull", "engineAny": "engine", "driveAny": "drive", "coolingAny": "cooling",
    "extrasAny": "extras", "keelAny": "keel", "storageAny": "storage",
    "rigAny": "rig", "waterAny": "water",
}

# Boats chosen to cover the axes that interact, not to be representative:
# twin saildrive, single shaft, trailered outboard, twin petrol sterndrive.
VESSELS = {
    "Lagoon 42": dict(
        hull="catamaran_sail", engine="diesel", drive="saildrive", cooling="raw_water",
        keel="none", rig="sloop", storage="afloat_year_round", water="salt",
        engines=2,
        extras=["fresh_water", "heads", "fridge", "cabin", "instruments", "genset", "watermaker", "liferaft", "epirb", "furler",
                "windlass", "holding_tank", "shore_power", "aircon",
                "tender", "davits", "radar", "ais", "autopilot", "solar",
                "inverter", "passerelle", "offshore"],
    ),
    "Bavaria 38": dict(
        hull="monohull_sail", engine="diesel", drive="shaft", cooling="raw_water",
        keel="fin", rig="sloop", storage="hauled_winter", water="salt",
        engines=1,
        extras=["fresh_water", "heads", "fridge", "cabin", "instruments", "lpg", "furler", "windlass", "liferaft", "epirb",
                "shore_power", "offshore"],
    ),
    "Buster Magnum": dict(
        hull="motor", engine="petrol", drive="outboard", cooling="raw_water",
        keel="none", rig="none", storage="trailer", water="brackish",
        engines=1,
        extras=["trailer"],
    ),
    "Highfield 310 tender": dict(
        hull="rib", engine="petrol", drive="outboard", cooling="raw_water",
        keel="none", rig="none", storage="dry_stack", water="salt",
        engines=1, extras=[],
    ),
    # Reaches the lifting-keel and heater axes: a shoal-draft northern cruiser
    # that comes out of the water each winter.
    "Southerly 42 lift-keel": dict(
        hull="monohull_sail", engine="diesel", drive="saildrive",
        cooling="raw_water", keel="lifting", rig="sloop",
        storage="hauled_winter", water="salt", engines=1,
        extras=["fresh_water", "heads", "fridge", "cabin", "instruments",
                "heater", "solar", "wind_gen", "hydrogenerator", "autopilot",
                "radar", "ais", "inverter", "liferaft", "epirb", "furler",
                "windlass", "holding_tank", "shore_power", "lpg", "tender", "offshore"],
    ),
    # The only keel-cooled, dry-exhaust boat in the set. Steel canal cruiser,
    # fresh water, afloat all year - nothing else in the fleet is like it.
    "Steel canal cruiser": dict(
        hull="motor", engine="diesel", drive="shaft", cooling="keel_cooled",
        keel="none", rig="none", storage="afloat_year_round", water="fresh",
        engines=1,
        extras=["fresh_water", "heads", "fridge", "cabin", "instruments",
                "heater", "solar", "inverter", "shore_power", "holding_tank"],
    ),
    "Twin sterndrive 11 m": dict(
        hull="motor", engine="petrol", drive="sterndrive", cooling="raw_water",
        keel="none", rig="none", storage="hauled_winter", water="salt",
        engines=2,
        # An 11 m cruiser has an interior; leaving "cabin" off this persona hid
        # the fact that cabin nodes were reaching boats that have no cabin.
        extras=["cabin", "instruments", "fresh_water", "heads", "fridge",
                "thruster", "genset", "shore_power", "holding_tank"],
    ),
}


def applies(node, v):
    for key, values in node.get("appliesTo", {}).items():
        got = v[RULE_FIELD[key]]
        got = got if isinstance(got, list) else [got]
        if not set(values) & set(got):
            return False
    return True


def expand(catalog, v):
    """Component code -> how many instances this boat has."""
    out = Counter()
    for cat in catalog["categories"]:
        if not applies(cat, v):
            continue
        cat_per = cat.get("perEngine", False)
        for comp in cat["components"]:
            if not applies(comp, v):
                continue
            per = comp.get("perEngine", cat_per)
            out[comp["code"]] = v["engines"] if per else 1
    return out


def expand_checklists(lists, v):
    return {cl["code"]: [i for i in cl["items"] if applies(i, v)]
            for cl in lists["checklists"]}


# (vessel, kind, target, expected) - `kind` says how to read `target`
CHECKS = [
    # --- twin saildrive: everything on the leg is doubled, the intake is not
    ("Lagoon 42", "count", "saildrive_anode", 2),
    ("Lagoon 42", "count", "saildrive_diaphragm", 2),
    ("Lagoon 42", "count", "saildrive_oil", 2),
    ("Lagoon 42", "count", "engine_oil", 2),
    ("Lagoon 42", "count", "raw_water_impeller", 2),
    ("Lagoon 42", "count", "sea_chest", 1),
    ("Lagoon 42", "absent", "stuffing_box", None),      # no shaft on a saildrive
    ("Lagoon 42", "absent", "drain_plug", None),         # not a trailered hull
    ("Lagoon 42", "present", "watermaker_membrane", None),
    # A diesel saildrive has glow plugs. It is recorded as `saildrive`,
    # which says nothing about fuel, so the component used to vanish.
    ("Lagoon 42", "count", "glow_plugs", 2),
    ("Bavaria 38", "count", "glow_plugs", 1),
    ("Twin sterndrive 11 m", "absent", "glow_plugs", None),
    ("Buster Magnum", "absent", "glow_plugs", None),

    # --- single shaft: the same parts, exactly one of each
    ("Bavaria 38", "count", "stuffing_box", 1),
    ("Bavaria 38", "count", "shaft_anode", 1),
    ("Bavaria 38", "count", "propeller", 1),
    ("Bavaria 38", "count", "engine_oil", 1),
    ("Bavaria 38", "present", "emergency_tiller", None),
    ("Bavaria 38", "present", "gas_leak_test", None),    # has lpg
    ("Bavaria 38", "absent", "saildrive", None),
    ("Bavaria 38", "absent", "blower", None),            # diesel, not petrol

    # --- trailered outboard: no drivetrain at all, and the plug is mandatory
    ("Buster Magnum", "cat_total", "drivetrain", 0),
    ("Buster Magnum", "cat_total", "standing_rig", 0),
    ("Buster Magnum", "cat_total", "running_rig", 0),
    ("Buster Magnum", "cat_total", "sails", 0),
    ("Buster Magnum", "cat_total", "gas", 0),
    ("Buster Magnum", "count", "drain_plug", 1),
    ("Buster Magnum", "count", "ob_gearcase_oil", 1),
    ("Buster Magnum", "present", "trailer_bearings", None),
    ("Buster Magnum", "absent", "blower", None),         # outboard, not inboard

    # --- a 3 m inflatable tender: the case that exposed "no rule means
    # everything". Every other boat here has an interior, so an unconditioned
    # interior node looked correct until this one arrived.
    ("Highfield 310 tender", "absent", "water_tank", None),
    ("Highfield 310 tender", "absent", "water_heater", None),
    ("Highfield 310 tender", "absent", "heads_pump", None),
    ("Highfield 310 tender", "absent", "fridge", None),
    ("Highfield 310 tender", "absent", "berths", None),
    ("Highfield 310 tender", "absent", "seacocks", None),
    ("Highfield 310 tender", "absent", "rudder_bearings", None),
    ("Highfield 310 tender", "absent", "house_batteries", None),
    ("Highfield 310 tender", "absent", "bilge_pump_auto", None),
    ("Highfield 310 tender", "absent", "transducers", None),
    ("Highfield 310 tender", "absent", "anchor_chain", None),
    ("Highfield 310 tender", "absent", "emergency_tiller", None),
    # THE TENDER PARADOX, v20. The `tender` category models a tender as
    # equipment of a mother ship, so a RIB that IS the vessel was cut off from
    # the category holding its own tubes: the dashboard showed the valves
    # overdue while the tree said that category does not apply. Marine practice
    # settles it - on a RIB the tubes are the hull - so the tubes moved to
    # `hull` on the hull axis, and `tender` stayed the mother ship's view.
    ("Highfield 310 tender", "present", "tubes", None),
    ("Highfield 310 tender", "present", "tube_valves", None),
    ("Highfield 310 tender", "absent", "tender_hull", None),
    ("Highfield 310 tender", "absent", "tender_valves", None),
    # Its engine arrives on its own drive axis, not as somebody's dinghy motor.
    ("Highfield 310 tender", "present", "ob_gearcase_oil", None),
    ("Highfield 310 tender", "absent", "tender_outboard", None),
    # The other side of the same coin: the cat carries a dinghy as equipment
    # and has no tubes of its own.
    ("Lagoon 42", "present", "tender_hull", None),
    ("Lagoon 42", "present", "tender_valves", None),
    ("Lagoon 42", "present", "tender_outboard", None),
    ("Lagoon 42", "absent", "tubes", None),
    ("Lagoon 42", "absent", "tube_valves", None),
    # A hard boat is neither: no tubes, no dinghy declared.
    ("Buster Magnum", "absent", "tubes", None),
    # A 3 m inflatable has no deck and no interior. Every line below was
    # reaching it until v18, because none of these nodes carried a rule.
    ("Highfield 310 tender", "absent", "deck", None),
    ("Highfield 310 tender", "absent", "teak_deck", None),
    ("Highfield 310 tender", "absent", "nonskid", None),
    ("Highfield 310 tender", "absent", "deck_rails_lifelines", None),
    ("Highfield 310 tender", "absent", "bathing_platform", None),
    ("Highfield 310 tender", "absent", "anchor_locker", None),
    ("Highfield 310 tender", "absent", "sprayhood_bimini", None),
    ("Highfield 310 tender", "absent", "hatches_portlights", None),
    ("Highfield 310 tender", "absent", "washboards", None),
    ("Highfield 310 tender", "absent", "stove", None),
    ("Highfield 310 tender", "absent", "washing_machine", None),
    ("Highfield 310 tender", "absent", "interior_lighting", None),
    ("Highfield 310 tender", "cat_total", "galley", 0),
    # An open sport boat has a deck but no cabin: the two axes are separate,
    # and gating everything on the hull would have lost that.
    ("Buster Magnum", "present", "nonskid", None),
    ("Buster Magnum", "absent", "stove", None),
    ("Buster Magnum", "absent", "interior_lighting", None),
    # A bowsprit is sailing gear, so it must not follow the deck rule.
    ("Buster Magnum", "absent", "bowsprit", None),
    ("Bavaria 38", "present", "bowsprit", None),

    # EVERY boat has cleats and fairleads. This node sat in `running_rig`,
    # which is gated on sailing hulls, so four of the seven boats here had
    # nothing to tie a line to - an 11 m motor yacht could not moor. Same
    # shape as the tender paradox: a node filed under a category whose rule
    # does not describe the node.
    ("Buster Magnum", "present", "mooring_cleats", None),
    ("Highfield 310 tender", "present", "mooring_cleats", None),
    ("Steel canal cruiser", "present", "mooring_cleats", None),
    ("Twin sterndrive 11 m", "present", "mooring_cleats", None),
    ("Bavaria 38", "present", "mooring_cleats", None),
    # And a boat hook, from the tender upwards.
    ("Highfield 310 tender", "present", "boat_hook", None),
    ("Lagoon 42", "present", "boat_hook", None),

    # A safety padeye is NOT mooring hardware and must not follow it. It is
    # where a jackstay or a harness tether is shackled, so it fails a person
    # overboard rather than a warp - which is why it left the cleats node and
    # took HIGH criticality with it.
    ("Bavaria 38", "present", "safety_padeyes", None),
    ("Lagoon 42", "present", "safety_padeyes", None),
    ("Buster Magnum", "absent", "safety_padeyes", None),
    ("Highfield 310 tender", "absent", "safety_padeyes", None),
    ("Steel canal cruiser", "absent", "safety_padeyes", None),
    # ⚠️ A bluewater motor yacht DOES carry jacklines, and this line is the
    # one that will be wrong first. It is correct only until the `offshore`
    # extras axis exists - see taxonomy.md §5.3.
    ("Twin sterndrive 11 m", "absent", "safety_padeyes", None),

    # Storm tactics (drogue, sea_anchor): offshore-equipped vessels only
    ("Bavaria 38", "present", "drogue", None),
    ("Lagoon 42", "present", "drogue", None),
    ("Southerly 42 lift-keel", "present", "drogue", None),
    ("Twin sterndrive 11 m", "absent", "drogue", None),
    ("Buster Magnum", "absent", "drogue", None),
    ("Highfield 310 tender", "absent", "drogue", None),
    ("Steel canal cruiser", "absent", "drogue", None),

    ("Bavaria 38", "present", "sea_anchor", None),
    ("Lagoon 42", "present", "sea_anchor", None),
    ("Southerly 42 lift-keel", "present", "sea_anchor", None),
    ("Twin sterndrive 11 m", "absent", "sea_anchor", None),
    ("Highfield 310 tender", "absent", "sea_anchor", None),

    # Jackstays and harnesses: sailing hulls only
    ("Bavaria 38", "present", "jackstays", None),
    ("Lagoon 42", "present", "jackstays", None),
    ("Southerly 42 lift-keel", "present", "jackstays", None),
    ("Twin sterndrive 11 m", "absent", "jackstays", None),
    ("Buster Magnum", "absent", "jackstays", None),
    ("Highfield 310 tender", "absent", "jackstays", None),
    ("Bavaria 38", "present", "harnesses_tethers", None),
    ("Twin sterndrive 11 m", "absent", "harnesses_tethers", None),
    ("Highfield 310 tender", "absent", "harnesses_tethers", None),

    # MOB equipment: decked hulls only (tender has none)
    ("Lagoon 42", "present", "mob_equipment", None),
    ("Bavaria 38", "present", "mob_equipment", None),
    ("Twin sterndrive 11 m", "present", "mob_equipment", None),
    ("Steel canal cruiser", "present", "mob_equipment", None),
    ("Buster Magnum", "present", "mob_equipment", None),
    ("Highfield 310 tender", "absent", "mob_equipment", None),

    # Engine bay automatic fire system: inboards only (outboard boats have none)
    ("Lagoon 42", "present", "engine_fire_system", None),
    ("Bavaria 38", "present", "engine_fire_system", None),
    ("Twin sterndrive 11 m", "present", "engine_fire_system", None),
    ("Steel canal cruiser", "present", "engine_fire_system", None),
    ("Buster Magnum", "absent", "engine_fire_system", None),
    ("Highfield 310 tender", "absent", "engine_fire_system", None),

    # Abandon ship & SART: liferaft-carrying vessels only (dayboats have none)
    ("Lagoon 42", "present", "grab_bag", None),
    ("Bavaria 38", "present", "grab_bag", None),
    ("Southerly 42 lift-keel", "present", "grab_bag", None),
    ("Twin sterndrive 11 m", "absent", "grab_bag", None),
    ("Buster Magnum", "absent", "grab_bag", None),
    ("Highfield 310 tender", "absent", "grab_bag", None),
    ("Steel canal cruiser", "absent", "grab_bag", None),

    ("Bavaria 38", "present", "sart", None),
    ("Lagoon 42", "present", "sart", None),
    ("Twin sterndrive 11 m", "absent", "sart", None),
    ("Buster Magnum", "absent", "sart", None),
    ("Steel canal cruiser", "absent", "sart", None),

    # Emergency VHF antenna: sailing rigs only (dismasting scenario)
    ("Bavaria 38", "present", "emergency_vhf_antenna", None),
    ("Lagoon 42", "present", "emergency_vhf_antenna", None),
    ("Southerly 42 lift-keel", "present", "emergency_vhf_antenna", None),
    ("Twin sterndrive 11 m", "absent", "emergency_vhf_antenna", None),
    ("Buster Magnum", "absent", "emergency_vhf_antenna", None),
    ("Steel canal cruiser", "absent", "emergency_vhf_antenna", None),
    ("Highfield 310 tender", "absent", "emergency_vhf_antenna", None),

    # Stern-to in the Med is what a passerelle is for; a trailered sport boat
    # and a canal cruiser have none.
    ("Lagoon 42", "present", "passerelle", None),
    ("Buster Magnum", "absent", "passerelle", None),
    # What it does have: an outboard, and the safety kit that fits in it.
    ("Highfield 310 tender", "count", "ob_gearcase_oil", 1),
    ("Highfield 310 tender", "count", "ob_spark_plugs", 1),
    ("Highfield 310 tender", "count", "ob_flush", 1),
    ("Highfield 310 tender", "present", "lifejackets", None),
    ("Highfield 310 tender", "present", "fire_extinguishers", None),
    # NOT tender_valves: the `tender` category models the tender as EQUIPMENT
    # of a mother ship (extrasAny: tender, davits). A RIB registered as its own
    # vessel therefore does not reach its own tube and valve checks, because
    # `appliesTo` ANDs across dimensions and cannot say "rib hull OR carries a
    # tender". A real limit of the grammar, recorded in taxonomy.md 5.2 - not
    # worth inventing an `anyOf` group for one category today.
    ("Highfield 310 tender", "absent", "tender_valves", None),

    # --- twin petrol sterndrive: legs doubled, thruster and blower shared
    ("Twin sterndrive 11 m", "count", "sterndrive_bellows", 2),
    ("Twin sterndrive 11 m", "count", "sterndrive_oil", 2),
    ("Twin sterndrive 11 m", "count", "engine_oil", 2),
    ("Twin sterndrive 11 m", "count", "thruster", 1),
    ("Twin sterndrive 11 m", "count", "thruster_anode", 1),
    ("Twin sterndrive 11 m", "present", "blower", None),
    ("Twin sterndrive 11 m", "count", "blower", 1),
    ("Twin sterndrive 11 m", "present", "gen_enclosure", None),
]

# Checklist items that must reach a given boat, because their absence is the
# kind of silence that gets someone hurt.
CHECKLIST_CHECKS = [
    ("Buster Magnum", "commission", "cm_drain_plug"),
    ("Buster Magnum", "predeparture", "pd_drain_plug"),
    ("Buster Magnum", "winterize", "wz_ob_gearcase"),
    ("Buster Magnum", "safety", "sf_tiller"),
    ("Highfield 310 tender", "winterize", "wz_ob_gearcase"),
    ("Highfield 310 tender", "winterize", "wz_ob_flush"),
    ("Highfield 310 tender", "safety", "sf_lifejackets"),
    ("Twin sterndrive 11 m", "safety", "sf_blower"),
    ("Twin sterndrive 11 m", "predeparture", "pd_blower"),
    ("Bavaria 38", "safety", "sf_rigging_shears"),
    ("Bavaria 38", "safety", "sf_bungs"),
    ("Bavaria 38", "winterize", "wz_sails_off"),
    ("Lagoon 42", "winterize", "wz_watermaker"),
]


def main():
    catalog = json.load(io.open(CATALOG, encoding="utf-8"))
    lists = json.load(io.open(CHECKLISTS, encoding="utf-8"))
    trees = {name: expand(catalog, v) for name, v in VESSELS.items()}
    cats = {c["code"]: c for c in catalog["categories"]}
    failed = []

    print(f"{'vessel':22} {'nodes':>6} {'instances':>10}   checklists")
    print("-" * 78)
    for name, v in VESSELS.items():
        tree = trees[name]
        cl = expand_checklists(lists, v)
        counts = " ".join(f"{k[:4]}:{len(x)}" for k, x in cl.items())
        print(f"{name:22} {len(tree):6} {sum(tree.values()):10}   {counts}")

    print()
    for name, kind, target, want in CHECKS:
        tree = trees[name]
        if kind == "count":
            got = tree.get(target, 0)
            ok = got == want
            detail = f"{target} = {got}, want {want}"
        elif kind == "present":
            ok = target in tree
            detail = f"{target} present"
        elif kind == "absent":
            ok = target not in tree
            detail = f"{target} absent"
        elif kind == "cat_total":
            codes = {c["code"] for c in cats[target]["components"]}
            got = sum(n for c, n in tree.items() if c in codes)
            ok = got == want
            detail = f"category {target} = {got}, want {want}"
        if not ok:
            failed.append(f"{name}: {detail}")
            print(f"  FAIL  {name:22} {detail}")

    for name, listcode, item in CHECKLIST_CHECKS:
        cl = expand_checklists(lists, VESSELS[name])
        if item not in {i["code"] for i in cl[listcode]}:
            failed.append(f"{name}: {item} missing from {listcode}")
            print(f"  FAIL  {name:22} {item} missing from {listcode}")

    # Invariant, independent of the lists above: a per-engine component must
    # appear once per engine, and a shared one exactly once. This is what
    # `perEngine` on drivetrain was silently violating.
    for name, v in VESSELS.items():
        for cat in catalog["categories"]:
            if not applies(cat, v):
                continue
            for comp in cat["components"]:
                if not applies(comp, v):
                    continue
                per = comp.get("perEngine", cat.get("perEngine", False))
                want = v["engines"] if per else 1
                got = trees[name][comp["code"]]
                if got != want:
                    failed.append(f"{name}: {comp['code']} = {got}, want {want}")
                    print(f"  FAIL  {name:22} {comp['code']} = {got}, want {want}")

    # Coverage. 87 passing checks over a catalog where 24 of 323 nodes were
    # never evaluated by any persona is 87 checks plus a 7% blind spot: the
    # whole tender category, davits, radar, AIS, the autopilot, keel cooling
    # and the lifting keel had never once been decided either way. A persona
    # set that cannot reach a node cannot be wrong about it, and that reads as
    # green. Every node must be reachable by at least one boat.
    unreached = []
    for cat in catalog["categories"]:
        for comp in cat["components"]:
            if not any(applies(comp, v) and applies(cat, v)
                       for v in VESSELS.values()):
                unreached.append(comp["code"])
    if unreached:
        print()
        print("  %d node(s) no persona can reach - add a boat, or an extra "
              "to an existing one:" % len(unreached))
        for code in sorted(unreached):
            print("    " + code)
        failed.extend(unreached)

    if failed:
        print(f"\n{len(failed)} failed")
        return 1
    print(f"{len(CHECKS) + len(CHECKLIST_CHECKS)} explicit checks pass, "
          f"per-engine cardinality holds on all {len(VESSELS)} boats, "
          f"every node reachable")
    return 0


if __name__ == "__main__":
    sys.exit(main())
