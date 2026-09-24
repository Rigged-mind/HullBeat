"""Labels for the vessel profile - the nine axes and every value they take.

The catalog carries the VOCABULARY (`profileVocabulary` in catalog.json); it
has never carried the words a person reads. Nothing needed them until now,
because nothing ever asked an owner what boat they have - which is exactly
why the first run of the built app offered an empty screen and a button to
invent a node by hand.

They live here rather than in Kotlin for the usual reason: a Cyrillic string
typed into a Composable is invisible to `check_translations.py` and dies in a
release build's resource shrink. `make_strings.py` turns these into
`axis_<name>` and `opt_<axis>_<value>` in both locales, and the Ukrainian
goes through the same review file as everything else.

⚠️ Every value in catalog.json's profileVocabulary must appear here, and
nothing else may: `make_strings.py` fails on either mismatch, so a new axis
value cannot ship without a word for it.
"""

AXES = {
    "hull": ("Hull type", "Тип корпусу"),
    "engine": ("Engine", "Двигун"),
    "drive": ("Drive", "Передача"),
    "cooling": ("Engine cooling", "Охолодження двигуна"),
    "rig": ("Rig", "Озброєння"),
    "keel": ("Keel", "Кіль"),
    "storage": ("Where she spends the winter", "Де зимує"),
    "water": ("Water", "Вода"),
    "extras": ("What she has aboard", "Що є на борту"),
}

# (axis, value): (English, Ukrainian)
VALUES = {
    ("hull", "monohull_sail"): ("Sailing monohull", "Вітрильний однокорпусник"),
    ("hull", "catamaran_sail"): ("Sailing catamaran", "Вітрильний катамаран"),
    ("hull", "trimaran_sail"): ("Sailing trimaran", "Вітрильний тримаран"),
    ("hull", "motor"): ("Motor boat", "Моторне судно"),
    ("hull", "rib"): ("RIB", "РІБ (жорстко-надувний)"),
    ("hull", "dinghy"): ("Dinghy or tender", "Тузик або шлюпка"),

    ("engine", "diesel"): ("Diesel", "Дизель"),
    ("engine", "petrol"): ("Petrol", "Бензиновий"),
    ("engine", "electric"): ("Electric", "Електричний"),
    ("engine", "none"): ("No engine", "Без двигуна"),

    ("drive", "shaft"): ("Shaft", "Валопровід"),
    ("drive", "saildrive"): ("Saildrive", "Сейлдрайв"),
    ("drive", "sterndrive"): ("Sterndrive", "Поворотно-відкидна колонка"),
    ("drive", "outboard"): ("Outboard", "Підвісний мотор"),
    ("drive", "none"): ("No drive", "Без передачі"),

    ("cooling", "raw_water"): ("Raw water", "Забортною водою"),
    ("cooling", "keel_cooled"): ("Keel cooled, dry exhaust",
                                 "Кільове, сухий вихлоп"),

    ("rig", "none"): ("No rig", "Без вітрил"),
    ("rig", "sloop"): ("Sloop", "Шлюп"),
    ("rig", "cutter"): ("Cutter", "Тендер"),
    ("rig", "ketch"): ("Ketch", "Кеч"),
    ("rig", "yawl"): ("Yawl", "Йол"),
    ("rig", "schooner"): ("Schooner", "Шхуна"),
    ("rig", "cat_rig"): ("Cat rig", "Кет"),
    ("rig", "junk"): ("Junk rig", "Джонка"),

    ("keel", "fin"): ("Fin keel", "Плавниковий"),
    ("keel", "long"): ("Long keel", "Довгий"),
    ("keel", "bilge"): ("Bilge keels", "Скулові"),
    ("keel", "lifting"): ("Lifting keel", "Підіймальний"),
    ("keel", "centreboard"): ("Centreboard", "Шверт"),
    ("keel", "daggerboard"): ("Daggerboard", "Кинджальний шверт"),
    ("keel", "none"): ("No keel", "Без кіля"),

    ("storage", "afloat_year_round"): ("Afloat all year", "На плаву цілий рік"),
    ("storage", "hauled_winter"): ("Hauled out for winter",
                                   "Підіймається на зиму"),
    ("storage", "dry_stack"): ("Dry stack", "Сухий стелаж"),
    ("storage", "trailer"): ("On a trailer", "На причепі"),

    ("water", "salt"): ("Salt", "Солона"),
    ("water", "brackish"): ("Brackish", "Солонувата"),
    ("water", "fresh"): ("Fresh", "Прісна"),

    ("extras", "genset"): ("Generator", "Дизель-генератор"),
    ("extras", "watermaker"): ("Watermaker", "Опріснювач"),
    ("extras", "aircon"): ("Air conditioning", "Кондиціонер"),
    ("extras", "heater"): ("Cabin heater", "Опалювач каюти"),
    ("extras", "thruster"): ("Bow or stern thruster", "Підрулювальний пристрій"),
    ("extras", "davits"): ("Davits", "Шлюпбалки"),
    ("extras", "passerelle"): ("Passerelle", "Трап-пасарель"),
    ("extras", "tender"): ("Tender", "Тузик"),
    ("extras", "trailer"): ("Trailer", "Причеп"),
    ("extras", "holding_tank"): ("Holding tank", "Фекальний бак"),
    ("extras", "lpg"): ("Gas system", "Газова система"),
    ("extras", "solar"): ("Solar panels", "Сонячні панелі"),
    ("extras", "wind_gen"): ("Wind generator", "Вітрогенератор"),
    ("extras", "inverter"): ("Inverter", "Інвертор"),
    ("extras", "autopilot"): ("Autopilot", "Автопілот"),
    ("extras", "radar"): ("Radar", "Радар"),
    ("extras", "ais"): ("AIS", "АІС"),
    ("extras", "liferaft"): ("Liferaft", "Рятувальний пліт"),
    ("extras", "epirb"): ("EPIRB", "Аварійний радіобуй"),
    ("extras", "shore_power"): ("Shore power", "Берегове живлення"),
    ("extras", "furler"): ("Headsail furler", "Закрутка стакселя"),
    ("extras", "windlass"): ("Windlass", "Брашпиль"),
    ("extras", "hydrogenerator"): ("Hydrogenerator", "Гідрогенератор"),
    ("extras", "fresh_water"): ("Fresh water system", "Система прісної води"),
    ("extras", "heads"): ("Marine heads", "Гальюн"),
    ("extras", "fridge"): ("Fridge or freezer", "Холодильник"),
    ("extras", "cabin"): ("Cabin", "Каюта"),
    ("extras", "instruments"): ("Instruments", "Прилади"),
    # The one extra that is not a fitting but a way of using the boat: it
    # gates the storm gear, so it is asked as plainly as the rest.
    ("extras", "offshore"): ("Offshore passages",
                             "Переходи у відкритому морі"),
}
