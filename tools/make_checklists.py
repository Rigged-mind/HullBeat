#!/usr/bin/env python3
"""Build catalog/checklists.json and the uk translations for seasonal procedures.

A checklist is not a text list with ticks. Every item may point at a catalog
component, and completing an item that *is* that component's scheduled service
writes a ServiceRecord and resets DueCalculator. Items that merely protect or
prepare something do not - see `logs` below.

Content lives here so that every node reference is checked against the catalog
at build time; a typo in a code fails the build instead of shipping a dead link.

Run:  python tools/make_checklists.py
Exit: 1 if any node code, profile value or ordering rule is wrong.
"""
import io
import json
import os
import sys
from collections import Counter

CATALOG = os.path.join("catalog", "catalog.json")
UK = os.path.join("catalog", "i18n", "uk.json")
# Source of truth only; app/build.gradle.kts copies it into assets at build time.
OUT = os.path.join("catalog", "checklists.json")

SAIL = ["monohull_sail", "catamaran_sail", "trimaran_sail"]
# "Has an engine down below": engine oil, fuel filters, the closed coolant
# circuit, the raw-water circuit. An outboard carries all of that inside
# itself and has its own items, so this is a DRIVE question, not a fuel one.
INBOARD = ["shaft", "saildrive", "sterndrive"]
# Hulls with an interior and through-hull fittings. A RIB or a dinghy has
# neither, and an item with no rule at all reaches them anyway - which is
# how a 3 m tender came to be told to bypass its calorifier.
DECKED = ["monohull_sail", "catamaran_sail", "trimaran_sail", "motor"]

# (code, node|None, logs, appliesTo, en, uk)
#
# `logs=True` means: finishing this item IS the scheduled service for that node,
# so it writes a record. `logs=False` means the item protects or prepares the
# node without servicing it - draining a circuit is not a service, and treating
# it as one would silently push the real interval a year into the future.
#
# True is not the whole answer though: the builder turns it into "record" or
# "expiry" depending on whether the catalog marks that component `expires`.
# See the comment at the row-building step for why a boolean would be a bug.
WINTERIZE = [
    # --- engine and drivetrain -------------------------------------------
    ("wz_engine_oil", "engine_oil", True, {"driveAny": INBOARD},
     "Change engine oil and filter before lay-up, not in spring",
     "Замінити оливу й фільтр до консервації, а не навесні"),
    # The reverse gear belongs to a shaft installation; a sterndrive and a
    # saildrive each carry their own oil, covered by their own items.
    ("wz_gearbox_oil", "gearbox_oil", True, {"driveAny": ["shaft"]},
     "Change gearbox oil",
     "Замінити оливу в редукторі"),
    ("wz_saildrive_oil", "saildrive_oil", True, {"driveAny": ["saildrive"]},
     "Change saildrive oil; check it for water",
     "Замінити оливу в сейлдрайві, перевірити на воду"),
    ("wz_fuel_prefilter", "fuel_prefilter", True, {"driveAny": INBOARD},
     "Change the primary filter and drain water from the bowl",
     "Замінити фільтр-сепаратор, злити воду зі склянки"),
    ("wz_fuel_full", "fuel_tank", False, {"driveAny": INBOARD},
     "Fill the tank to about 95% and add stabiliser or biocide",
     "Заправити бак приблизно на 95%, додати стабілізатор або біоцид"),
    ("wz_raw_flush", "raw_water_strainer", False, {"coolingAny": ["raw_water"], "driveAny": INBOARD},
     "Flush the raw-water circuit with fresh water",
     "Промити контур забортної води прісною"),
    ("wz_raw_antifreeze", "raw_water_hoses", False,
     {"hullAny": DECKED, "coolingAny": ["raw_water"], "driveAny": INBOARD},
     "Draw antifreeze through until it runs from the exhaust",
     "Заповнити контур забортної води судновим антифризом, доки він не вийде з вихлопного патрубка"),
    ("wz_coolant_check", "coolant", False, {"driveAny": INBOARD},
     "Check antifreeze strength in the closed circuit",
     "Перевірити концентрацію антифризу в закритому контурі"),
    ("wz_impeller_out", "raw_water_impeller", False, {"coolingAny": ["raw_water"], "driveAny": INBOARD},
     "Remove the impeller, rinse it, grease it with silicone and bag it beside the pump; tag the helm so nobody starts the engine",
     "Зняти імпелер, промити, змастити силіконом і покласти в пакет біля помпи; повісити табличку на пост, щоб не запустили двигун"),
    ("wz_stuffing_box", "stuffing_box", False, {"driveAny": ["shaft"]},
     "Grease the stuffing box or check the dripless bellows date",
     "Набити сальник або перевірити дату діафрагми (гумового сильфона)"),
    ("wz_ob_flush", "ob_flush", False, {"driveAny": ["outboard"]},
     "Flush the outboard, then fog the cylinders and run the fuel system dry",
     "Промити підвісний мотор, потім законсервувати циліндри спреєм і випрацювати пальне до зупинки двигуна (виробити залишок з поплавкової камери)"),
    ("wz_ob_gearcase", "ob_gearcase_oil", True, {"driveAny": ["outboard"]},
     "Change gearcase oil; milky oil means a seal has gone",
     "Замінити оливу в нозі; біла емульсія означає, що пробило сальник"),

    # --- fresh water and sanitation ---------------------------------------
    ("wz_water_drain", "water_tank", False, {"extrasAny": ["fresh_water"]},
     "Drain tanks and lines, or push potable antifreeze through",
     "Злити воду з баків і магістралей або заповнити систему нетоксичним судновим антифризом"),
    ("wz_water_heater", "water_heater", False, {"extrasAny": ["fresh_water"]},
     "Drain and bypass the calorifier",
     "Злити воду з бойлера й закольцувати магістралі в обхід нього (bypass)"),
    ("wz_heads", "heads_pump", False, {"extrasAny": ["heads"]},
     "Shut the heads intake seacock, drop the intake hose into the pink propylene glycol and pump it through the bowl into the tank",
     "Перекрити вхідний кінгстон гальюна, опустити шланг забору в нетоксичний (пропіленгліколевий) антифриз і прокачати через чашу в бак — колір видно на виході"),
    ("wz_holding", "holding_tank", False, {"extrasAny": ["holding_tank"]},
     "Empty and rinse the holding tank",
     "Спорожнити й промити фекальний бак"),
    ("wz_bilge", "bilge_cleanliness", False, {"hullAny": DECKED},
     "Pump the bilge dry and clean it; water left in it freezes",
     "Осушити й вимити трюм; залишена вода замерзне"),
    ("wz_watermaker", "watermaker_membrane", True, {"extrasAny": ["watermaker"]},
     "Pickle the membrane",
     "Законсервувати мембрану"),

    # --- electrical --------------------------------------------------------
    ("wz_batteries", "house_batteries", False, {"hullAny": DECKED},
     "Charge fully, then take them ashore or leave a maintainer on",
     "Зарядити повністю, потім зняти на берег або лишити на підзарядці"),
    ("wz_terminals", "battery_terminals", False, {"hullAny": DECKED},
     "Clean and grease the terminals",
     "Почистити й змастити клеми"),
    ("wz_switches", "main_switches", False, {"hullAny": DECKED},
     "Switch everything off except the bilge pump if she stays afloat",
     "Вимкнути все, крім трюмної помпи, якщо човен лишається на воді"),
    ("wz_shore", "shore_cable", False, {"extrasAny": ["shore_power"]},
     "Check the shore cable and inlet for heat damage before leaving it live all winter",
     "Оглянути береговий кабель і розетку на сліди перегріву — вони лишаться під напругою всю зиму"),

    # --- hull and underwater ----------------------------------------------
    ("wz_wash", "hull_cleaning", True, {"storageAny": ["hauled_winter", "dry_stack", "trailer"]},
     "Pressure-wash the hull the hour she comes out, before growth dries on",
     "Помити корпус тієї ж години, коли підняли, поки обростання не присохло"),
    ("wz_anodes_note", "anodes_hull", False, {"hullAny": DECKED},
     "Photograph the anodes and note how much is left",
     "Сфотографувати аноди й записати, скільки лишилось"),
    ("wz_seacocks_open", "seacocks", False,
     {"hullAny": DECKED,
      "storageAny": ["hauled_winter", "dry_stack", "trailer"]},
     "Set every seacock half open, not fully open: a ball valve at either "
     "end of its travel still holds water in the seat, and that is what "
     "splits the casting",
     "Встановити кінгстони (забортні клапани) у напіввідкрите положення (45°): "
     "у крайніх положеннях куля однаково тримає воду в сідлі, і саме вона "
     "розриває корпус крана"),
    ("wz_seacocks_shut", "seacocks", False,
     {"hullAny": DECKED, "storageAny": ["afloat_year_round"]},
     "Close every seacock except the ones the bilge pump needs",
     "Закрити всі кінгстони, крім потрібних трюмній помпі"),
    ("wz_transducer", "transducers", False, {"extrasAny": ["instruments"]},
     "Pull the paddlewheel log and fit the blank",
     "Вийняти крильчатку лага, встановити штатну заглушку крізькорпусного штуцера"),
    ("wz_rudder", "rudder_bearings", False, {"hullAny": DECKED},
     "Check rudder bearing play and write the figure down for comparison",
     "Перевірити люфт балера й записати число, щоб було з чим порівняти"),

    # --- rig and sails -----------------------------------------------------
    ("wz_sails_off", "sail_wash", False, {"hullAny": SAIL},
     "Take the sails off and dry them fully before folding",
     "Зняти вітрила й повністю висушити перед складанням"),
    ("wz_sail_service", "sail_service", False, {"hullAny": SAIL},
     "Send anything that needs stitching now, not in April",
     "Віддати на ремонт те, що цього потребує, зараз, а не у квітні"),
    ("wz_halyards", "halyards", False, {"hullAny": SAIL},
     "Wash the halyards, replace them with messengers, and store them dry",
     "Промити фали прісною водою, протягнути замість них провідники (пілотки) і прибрати на сухе зберігання"),
    ("wz_furler", "headsail_furler", False,
     {"hullAny": SAIL, "extrasAny": ["furler"]},
     "Rinse the furler with fresh water and work it through its full travel",
     "Промити закрутку прісною водою й прокрутити на весь хід"),
    ("wz_rig_slack", "turnbuckles", False, {"hullAny": SAIL},
     "Slacken the rig a little if the mast stays stepped",
     "Послабити натяг стоячого такелажу, якщо щогла залишається встановленою на зиму"),

    # --- deck and ground tackle -------------------------------------------
    ("wz_lines", "mooring_lines", False, {},
     "Wash and dry the mooring lines and fenders; store them out of sunlight",
     "Помити й висушити швартови й кранці, зберігати подалі від сонця"),
    ("wz_chain", "anchor_chain", False, {"hullAny": DECKED},
     "Wash the chain, and end-for-end it if the working end is worn",
     "Промити ланцюг, перевернути кінцями, якщо робочий кінець зношений"),

    # --- interior and gas --------------------------------------------------
    ("wz_gas_off", "gas_bottles", False, {"extrasAny": ["lpg"]},
     "Close the bottle, disconnect it, and check the locker drain is clear",
     "Закрити балон, відʼєднати, перевірити, що дренаж рундука не забитий"),
    ("wz_fridge", "fridge_seals", False, {"extrasAny": ["fridge"]},
     "Empty the fridge and prop the door open",
     "Спорожнити холодильник і лишити дверцята прочиненими"),
    ("wz_soft", "berths", False, {"extrasAny": ["cabin"]},
     "Take ashore every cushion, sail bag and anything else that holds damp",
     "Зняти на берег усі матраци, мішки й усе, що тримає вологу"),
    ("wz_vent", "condensation", False, {"extrasAny": ["cabin"]},
     "Set up ventilation or a dehumidifier; a sealed boat grows mould",
     "Забезпечити припливну вентиляцію або встановити осушувач (закупорене судно вкривається пліснявою)"),

    # --- paperwork ---------------------------------------------------------
    ("wz_expiry", "insurance", False, {},
     "Note every expiry that falls before next season: insurance, liferaft, flares",
     "Виписати все, що спливає до наступного сезону: страховка, пліт, піротехніка"),
]

COMMISSION = [
    # --- before she goes in ------------------------------------------------
    ("cm_hoses", "hose_clamps", True, {"hullAny": DECKED},
     "Squeeze every hose below the waterline and check both clamps on each",
     "Перевірити еластичність усіх шлангів нижче ватерлінії та затяжку обох хомутів на кожному штуцері"),
    ("cm_seacocks", "seacocks", True, {"hullAny": DECKED},
     "Work every seacock, grease it, and close them all before the lift",
     "Розходити кожен кінгстон, змастити й повністю закрити всі перед спуском на воду"),
    ("cm_anodes", "anodes_hull", True, {"hullAny": DECKED},
     "Replace anodes at 50% or less, and keep antifouling and grease off them and off their contact faces",
     "Замінити аноди, що зʼїдені наполовину, і не допустити необростайки чи мастила на них та на контактних поверхнях"),
    ("cm_shaft_anode", "shaft_anode", True, {"driveAny": ["shaft"]},
     "Replace the shaft anode",
     "Замінити анод на валу"),
    ("cm_saildrive_anode", "saildrive_anode", True, {"driveAny": ["saildrive"]},
     "Replace the saildrive anode and check the diaphragm date",
     "Замінити анод сейлдрайва, перевірити дату діафрагми"),
    ("cm_antifouling", "antifouling", True, {"storageAny": ["hauled_winter", "dry_stack", "trailer"]},
     "Antifoul; mask the anodes and the transducer face",
     "Нанести необростайку; заклеїти аноди й робочу поверхню датчика"),
    ("cm_keel_joint", "keel_joint", True, {"hullAny": SAIL},
     "Look along the keel joint for a crack opening up",
     "Оглянути кільовий шов (стик кіля з корпусом) на наявність розкриття тріщин або іржі"),
    ("cm_rudder", "rudder_bearings", False, {"hullAny": DECKED},
     "Compare rudder bearing play with the figure from lay-up",
     "Порівняти люфт балера з числом, записаним при консервації"),
    ("cm_gelcoat", "hull_gelcoat", False, {"hullAny": DECKED},
     "Polish and wax the topsides",
     "Відполірувати й навощити борти"),
    # The single most common way a boat is launched with a hole in it. It goes
    # before the transducer item because it happens on the trailer, not afloat.
    ("cm_drain_plug", "drain_plug", True, {"hullAny": ["motor", "rib", "dinghy"]},
     "Fit the transom drain plug and pull on it by hand; carry the spare",
     "Закрутити зливний корок транця й потягнути рукою; мати запасний із собою"),
    ("cm_transducer", "transducers", False, {"extrasAny": ["instruments"]},
     "Refit the paddlewheel; check the blank is out before you launch",
     "Встановити крильчатку лага на місце; переконатися, що заглушку знято до спуску на воду"),

    # --- engine ------------------------------------------------------------
    ("cm_impeller", "raw_water_impeller", True, {"coolingAny": ["raw_water"], "driveAny": INBOARD},
     "Fit a new impeller and count the blades of the old one",
     "Поставити новий імпелер і перерахувати лопаті старого"),
    ("cm_oil_check", "engine_oil", False, {"driveAny": INBOARD},
     "Check the oil level and look for water in it",
     "Перевірити рівень оливи й подивитись, чи немає в ній води"),
    ("cm_fuel_filter", "fuel_filter_fine", True, {"driveAny": INBOARD},
     "Change the fine filter and bleed the fuel system",
     "Замінити фільтр тонкої очистки й прокачати паливну систему"),
    ("cm_coolant", "coolant", False, {"driveAny": INBOARD},
     "Top up coolant and check the antifreeze strength",
     "Долити охолоджувальну рідину й перевірити концентрацію антифризу"),
    ("cm_belt", "alternator_belt", False, {"driveAny": INBOARD},
     "Check belt tension and look for black dust under it",
     "Перевірити натяг паса й подивитись, чи немає під ним чорного пилу"),
    ("cm_first_start", "exhaust_hose", False, {"coolingAny": ["raw_water"], "driveAny": INBOARD},
     "First start: water must show at the exhaust within 15 seconds, or stop",
     "Виконати перший пуск двигуна: забортна вода має піти з вихлопу впродовж 15 секунд, інакше негайно зупинити"),

    # --- the first fifteen minutes afloat ----------------------------------
    ("cm_leak_check", "through_hulls", False, {"hullAny": DECKED},
     "Lift the floorboards while she still hangs in the slings: check every seacock, the log fitting and the stern gland for weeping",
     "Підняти пайоли, доки судно ще висить у стропах: оглянути всі кінгстони, датчик лага та дейдвудний сальник на просочування води"),
    ("cm_bilge_pumps", "bilge_pump_auto", True, {"hullAny": DECKED},
     "Test both bilge pumps and lift the float switch by hand",
     "Перевірити обидві трюмні помпи й підняти поплавець рукою"),
    ("cm_bilge_alarm", "bilge_alarm", True, {"hullAny": DECKED},
     "Test the high water alarm; it is the one thing that wakes you",
     "Перевірити аварійну сигналізацію високого рівня води в трюмі (тест датчика та звукового зумера)"),

    # --- water, gas, safety -------------------------------------------------
    ("cm_watermaker_isolate", "watermaker", False, {"extrasAny": ["watermaker"]},
     "Isolate the watermaker before you chlorinate the tanks: free chlorine "
     "wrecks the membrane at a few ppm, and it does not recover",
     "Перекрити контур опріснювача перед дезінфекцією баків: вільний хлор "
     "необоротно руйнує мембрану навіть за концентрації в кілька ppm"),
    ("cm_water_sanitise", "water_tank_sanitise", True, {"extrasAny": ["fresh_water"]},
     "Refill and sanitise the fresh water system, then flush it twice",
     "Наповнити й продезінфікувати систему прісної води, потім двічі промити"),
    ("cm_water_heater", "water_heater", False, {"extrasAny": ["fresh_water"]},
     "Bring the calorifier back into circuit and check for leaks",
     "Повернути бойлер у контур і перевірити на течі"),
    ("cm_heads", "heads_pump", False, {"extrasAny": ["heads"]},
     "Flush the antifreeze out of the heads and check the joker valve",
     "Промити антифриз із гальюна й перевірити клапан-джокер"),
    ("cm_watermaker", "watermaker_membrane", True, {"extrasAny": ["watermaker"]},
     "De-pickle the membrane and run to waste until the taste is clean",
     "Розконсервувати мембрану й зливати, доки смак не стане чистим"),
    ("cm_gas_leak", "gas_leak_test", True, {"extrasAny": ["lpg"]},
     "Reconnect the bottle and leak-test every joint with soap solution",
     "Підʼєднати балон і перевірити кожне зʼєднання мильним розчином"),
    # Safety is 28 components, not three. It gets its own list (see SAFETY);
    # commissioning only points at it, so the two never drift apart.
    ("cm_safety_list", None, False, {},
     "Run the safety check — the whole list, not the three things you remember",
     "Пройти «Перевірку безпеки» — увесь список, а не три пункти, які памʼятаються"),

    # --- rig and sails -----------------------------------------------------
    ("cm_rig_walk", "turnbuckles", True, {"hullAny": SAIL},
     "Walk the rig: every split pin in place and taped, every terminal clean",
     "Оглянути стоячий такелаж: перевірити фіксацію шплінтів у талрепах (заклеїти стрічкою) і стан наконечників вант"),
    ("cm_masthead", "masthead", False, {"hullAny": SAIL},
     "Go up, or book someone to; check sheaves, wiring and the wind unit",
     "Піднятись або замовити підйом: перевірити шківи, проводку й датчик вітру"),
    ("cm_halyards", "halyards", False, {"hullAny": SAIL},
     "Pull the halyards back through and check for chafe at the sheaves",
     "Протягнути фали назад і перевірити потертості на шківах"),
    ("cm_sails_on", "mainsail", False, {"hullAny": SAIL},
     "Bend on the sails and pull each reef in on the mooring, not at sea",
     "Озброїти вітрила й перевірити взяття кожного рифу біля причалу, а не у відкритому морі"),

    # --- electronics and paperwork -----------------------------------------
    ("cm_nav_lights", "nav_lights", True, {},
     "Test every navigation light, including the ones you never use",
     "Перевірити всі ходові вогні, зокрема ті, якими ніколи не користуєтесь"),
    ("cm_vhf", "vhf", True, {"hullAny": DECKED},
     "Radio check on VHF, and test the DSC distress button on the test mode",
     "Провести перевірку звʼязку на УКХ і тест кнопки DSC у тестовому режимі"),
    ("cm_charts", "chart_updates", True, {"extrasAny": ["instruments"]},
     "Update the charts before the first passage, not during it",
     "Оновити карти до першого переходу, а не під час нього"),
    ("cm_insurance", "insurance", False, {},
     "Check insurance covers the cruising area you actually plan to use",
     "Перевірити, що страховка покриває той район, куди справді збираєтесь"),
]

# Safety kit does not care whether the boat was hauled out. A boat that stays
# afloat all year still has flares going out of date, so this list has its own
# annual schedule rather than living inside commissioning.
SAFETY = [
    # --- abandon ship -------------------------------------------------------
    ("sf_liferaft", "liferaft", False, {"extrasAny": ["liferaft"]},
     "Check the service date, and that the painter is tied to a strong point rather than the rail",
     "Перевірити дату чергового огляду плота; пусковий фалінь має бути закріплений за силовий рим, а не за леєр чи релінг"),
    ("sf_liferaft_hru", "liferaft_hru", True, {"extrasAny": ["liferaft"]},
     "Read the date moulded into the side of the hydrostatic release and enter it",
     "Зчитати дату, відлиту збоку на корпусі гідростата, й внести її"),
    ("sf_grab_bag", "grab_bag", True, {"extrasAny": ["liferaft"]},
     "Open the grab bag, check the dates inside, and put it back where it can be grabbed",
     "Відкрити аварійну сумку, перевірити дати всередині й покласти туди, звідки її легко схопити"),
    ("sf_plan", "emergency_plan", True, {"hullAny": DECKED},
     "Brief the crew: man overboard, fire, flooding, abandon ship — and say where everything is",
     "Провести інструктаж: людина за бортом, пожежа, надходження води, залишення судна — і сказати, де що лежить"),

    # --- staying on board ---------------------------------------------------
    ("sf_lifejackets", "lifejackets", True, {},
     "Inspect every jacket: bladder, webbing, crotch strap, whistle, light",
     "Оглянути кожен жилет: камеру, стропи, паховий ремінь, свисток, ліхтарик"),
    ("sf_lj_cylinders", "lifejacket_cylinders", True, {},
     "Weigh every CO2 cylinder on kitchen scales against its stamped gross weight; enter the earliest auto-cartridge date",
     "Зважити кожен балон CO₂ і звірити з вибитою повною масою (gross weight); внести найближчу дату заміни картриджа автоспуску"),
    ("sf_harnesses", "harnesses_tethers", True, {"hullAny": SAIL},
     "Pull hard on every stitch line, replace anything sun-bleached, and enter the label date",
     "Потягнути кожен шов на розрив і замінити вигоріле; внести дату з нашивки виробника"),
    ("sf_jackstays", "jackstays", True, {"hullAny": SAIL},
     "Check the jackstays end to end and the points they attach to",
     "Перевірити страхувальні леєри по всій довжині й точки, до яких вони кріпляться"),
    ("sf_pelican", "pelican_hooks", True, {"hullAny": SAIL},
     "Work the lifeline gate hooks; salt seizes them shut",
     "Розробити пеліканові гаки леєрної хвіртки; сіль їх заклинює"),
    ("sf_mob", "mob_equipment", True, {"hullAny": DECKED},
     "Check the danbuoy light, the horseshoe attachment, and that the sling actually deploys",
     "Перевірити вогонь рятувальної вішки (данбуя), кріплення круга/підкови та вільне розгортання рятувальної петлі (lifesling)"),

    # --- being found --------------------------------------------------------
    ("sf_epirb", "epirb", True, {"extrasAny": ["epirb"]},
     "Self-test the beacon and check it sits in its bracket the right way up",
     "Провести самотест буя й перевірити, що він стоїть у кронштейні правильним боком"),
    ("sf_epirb_battery", "epirb_battery", False, {"extrasAny": ["epirb"]},
     "Check the battery expiry; replacement is a service-centre job, so book it early",
     "Перевірити термін батареї; заміна робиться в сервісі, тому записуватись заздалегідь"),
    ("sf_epirb_reg", "epirb_registration", True, {"extrasAny": ["epirb"]},
     "Confirm the registration and emergency contacts; enter the expiry from the registration certificate",
     "Підтвердити реєстрацію та екстрені контакти; внести дату зі свідоцтва про реєстрацію"),
    ("sf_sart", "sart", True, {"extrasAny": ["liferaft"]},
     "Self-test the SART and enter the battery date from the label on its body",
     "Провести самотест SART і внести дату батареї з наліпки на корпусі"),
    ("sf_flares", "flares", False, {},
     "Check every flare is in date, and know where the expired ones can be handed in",
     "Записати найближчу дату з піротехніки й знати, куди здати прострочену"),
    ("sf_radar_reflector", "radar_reflector", True, {"hullAny": DECKED},
     "Check the reflector is mounted the way its maker specifies, not just tied on",
     "Перевірити, що відбивач закріплений так, як вимагає виробник, а не просто підвʼязаний"),
    ("sf_emerg_antenna", "emergency_vhf_antenna", True, {"hullAny": SAIL},
     "Check the emergency antenna and that its connector actually fits your set",
     "Перевірити аварійну антену і що її розʼєм справді підходить до вашої радіостанції"),
    ("sf_sound", "sound_signals", True, {},
     "Test the horn; check the bell and the whistle are aboard, not ashore",
     "Перевірити тифон; переконатись, що дзвін і свисток на борту, а не вдома"),

    # --- fire ---------------------------------------------------------------
    ("sf_extinguishers", "fire_extinguishers", True, {},
     "Check every gauge is in the green; enter the earliest date from the neck stamp or the label",
     "Перевірити, що всі манометри в зеленій зоні, порошкові перевернути й струсити; внести найранішу дату з горловини або шильдика"),
    ("sf_engine_fire", "engine_fire_system", True, {"driveAny": INBOARD},
     "Enter the date from the cylinder label; check the discharge port is not blocked",
     "Внести дату з шильдика балона; перевірити, що сопло не перекрите"),
    # Petrol vapour is heavier than air and sits in the bilge. Four minutes of
    # blower before the starter is the whole reason this fan exists, so a fan
    # that has quietly died is a fire item, not a comfort one.
    ("sf_blower", "blower", True, {"engineAny": ["petrol"], "driveAny": ["shaft", "saildrive", "sterndrive"]},
     "Run the blower and feel the outlet; check the ducting has not come adrift",
     "Увімкнути вентилятор моторного відсіку й перевірити потік рукою; оглянути, чи не відʼєднався рукав"),
    ("sf_fire_blanket", "fire_blanket", True, {"extrasAny": ["cabin"]},
     "Check the blanket is within reach of the stove — and not mounted behind it",
     "Перевірити, що протипожежне полотно в межах досяжності від плити, а не за нею"),
    ("sf_co_smoke", "co_smoke_detector", True, {"extrasAny": ["cabin"]},
     "Test the detectors and enter the sensor date from the back of the unit; the sensor expires, not just the battery",
     "Перевірити датчики й внести дату сенсора зі зворотного боку корпусу; спливає сам сенсор, а не лише батарейка"),

    # --- flooding and damage ------------------------------------------------
    ("sf_tiller", "emergency_tiller", True, {"hullAny": DECKED},
     "Dig the emergency tiller out, fit it to the rudder stock and steer with it",
     "Дістати аварійний румпель, поставити на балер і зробити пробне перекладання"),
    ("sf_bungs", "wooden_bungs", True, {"hullAny": DECKED},
     "Check a tapered bung of the right size hangs on its own seacock, not in a locker",
     "Перевірити наявність конусного дерев'яного корка (чопа) відповідного "
     "діаметра біля кожного кінгстона (підв'язаного до клапана)"),
    ("sf_damage_control", "damage_control", True, {"hullAny": DECKED},
     "Check the damage control kit: soft patch, tape, wedges",
     "Перевірити аварійний набір для пробоїн: пластир, стрічку, клини"),
    ("sf_emergency_tools", "emergency_tools", True, {},
     "Check the cutting tools are somewhere the crew can find them in the dark",
     "Перевірити, що різальний інструмент лежить там, де екіпаж знайде його потемки"),
    ("sf_rigging_shears", "rigging_shears", True, {"hullAny": SAIL},
     "Check the rig cutters work, and that everyone aboard knows where they are",
     "Перевірити, що різак такелажу ріже, і що всі на борту знають, де він"),
    ("sf_first_aid", "first_aid", True, {},
     "Restock the kit and enter the date of whichever medicine expires first",
     "Поповнити аптечку й внести дату препарату, що спливає найпершим"),

    # --- heavy weather ------------------------------------------------------
    ("sf_drogue", "drogue", True, {"extrasAny": ["offshore"]},
     "Open the drogue out; check the bridle and the points it loads (streamed astern, not from the bow)",
     "Оглянути плавуче гальмо (дрог): перевірити буксирну брагу та міцність точок кріплення "
     "(віддається з корми, не з носа)"),
    ("sf_sea_anchor", "sea_anchor", True, {"extrasAny": ["offshore"]},
     "Open the sea anchor out; check the rode, the swivel and the retrieval line",
     "Оглянути парашутний плавучий якір: перевірити якірний канат, вертлюг і витяжний кінець (retrieval line)"),
]

# The seasonal lists run twice a year. This one runs every time she leaves the
# dock, which makes it the only list that builds a habit - and the reason the
# app is in the owner's hand weekly rather than twice a season.
#
# Every item is `logs=False` on purpose. A departure check must NOT write a
# ServiceRecord: doing it forty times a season would bury the real maintenance
# history under noise. What gets stored is one ChecklistRun per departure.
PREDEPARTURE = [
    ("pd_drain_plug", "drain_plug", False, {"hullAny": ["motor", "rib", "dinghy"]},
     "Drain plug in and pulled on by hand",
     "Зливний корок транця вкручено й перевірено рукою"),
    ("pd_seacocks", "seacocks", False, {"hullAny": DECKED},
     "Engine and heads intake seacocks open",
     "Кінгстони двигуна й гальюна відкрито"),
    ("pd_oil", "engine_oil", False, {"driveAny": INBOARD},
     "Oil read off the dipstick and coolant off the expansion tank, not "
     "from memory",
     "Рівень оливи перевірено за щупом, охолоджувальної рідини — за "
     "розширювальним бачком, а не на памʼять"),
    ("pd_bilge", "bilge_float_switch", False, {"hullAny": DECKED},
     "Bilge dry and nothing fouling the float switch",
     "Трюм сухий, поплавковий вимикач помпи перевірено на вільний хід"),
    ("pd_blower", "blower", False, {"engineAny": ["petrol"], "driveAny": ["shaft", "saildrive", "sterndrive"]},
     "Blower run for four minutes, and the bilge sniffed before the starter",
     "Вентилятор моторного відсіку відпрацював 4 хвилини; відсутність випарів бензину в трюмі перевірено перед пуском стартера"),
    ("pd_fuel", "fuel_gauge", False, {"engineAny": ["diesel", "petrol"]},
     "Fuel for there, back and a third more checked, valves set",
     "Запас пального (правило третин: перехід, повернення, резерв) і паливні крани перевірено"),
    ("pd_lights", "nav_lights", False, {},
     "Navigation lights tested if there is any chance of coming back after dark",
     "Ходові навігаційні вогні перевірено (за будь-якої ймовірності ходу в сутінках або темряві)"),
    ("pd_lines", "mooring_lines", False, {},
     "Lines ready to slip and fenders where they will be needed",
     "Швартові кінці заведено на віддачу, кранці розвішано по потрібному борту"),
]

LISTS = [
    ("winterize", "autumn", "Winterization / lay-up", "Консервація",
     "infinitive", WINTERIZE),
    ("commission", "spring", "Spring commissioning", "Розконсервація",
     "infinitive", COMMISSION),
    ("safety", "any", "Safety check", "Перевірка безпеки",
     "infinitive", SAFETY),
    ("predeparture", "every", "Pre-departure check", "Перед виходом",
     "stative", PREDEPARTURE),
]

PROFILE_KEYS = {"hullAny": "hull", "engineAny": "engine",
                "driveAny": "drive", "coolingAny": "cooling",
                "extrasAny": "extras", "keelAny": "keel",
                "storageAny": "storage", "rigAny": "rig"}


def carry_review(previous):
    """(text -> confidence) for lines already read, so a rerun keeps them.

    Returns a function: give it a code and the new text, get back "ok" if
    that exact wording was approved before, "check" otherwise.
    """
    def verdict(code, text):
        old = previous.get(f"chk_{code}")
        if isinstance(old, list) and len(old) > 1 and old[0] == text:
            return old[1]
        return "check"
    return verdict


def main():
    catalog = json.load(io.open(CATALOG, encoding="utf-8"))
    comp = {c["code"]: c for cat in catalog["categories"] for c in cat["components"]}
    codes = set(comp)
    vocab = catalog["profileVocabulary"]

    failed = False
    # Read before the loops: the previous verdicts decide what stays approved.
    verdict = carry_review(
        json.load(io.open(UK, encoding="utf-8")).get("checklists", {}))
    out = {"version": 1, "catalogVersion": catalog["version"],
           "note": "Seasonal procedures. `node` links an item to a catalog "
                   "component; `logs` means finishing the item IS that "
                   "component's scheduled service, so it writes a record and "
                   "resets the interval.",
           "checklists": []}
    uk_terms = {}

    for code, season, en, uk, mood, items in LISTS:
        uk_terms[f"chk_{code}"] = [uk, verdict(code, uk)]
        rows = []
        seen = set()
        for i, (ic, node, logs, applies, ien, iuk) in enumerate(items):
            if ic in seen:
                print(f"    DUPLICATE item code: {ic}"); failed = True
            seen.add(ic)
            if node and node not in codes:
                print(f"    UNKNOWN node in {ic}: {node}"); failed = True
            for k, vals in applies.items():
                if k not in PROFILE_KEYS:
                    print(f"    UNKNOWN rule key in {ic}: {k}"); failed = True
                    continue
                bad = set(vals) - set(vocab[PROFILE_KEYS[k]])
                if bad:
                    print(f"    UNKNOWN {k} value in {ic}: {sorted(bad)}")
                    failed = True
            # No "uk" here. Translated text belongs in chk_<code> string
            # resources, generated by make_strings.py from uk.json - one
            # localisation mechanism, not two. `en` stays as the seed, the
            # same role Component.name plays for a node.
            row = {"code": ic, "order": (i + 1) * 10, "en": ien}
            if node:
                row["node"] = node
            # One item points at another list instead of a component.
            if ic == "cm_safety_list":
                row["link"] = "safety"
            if logs:
                # A boolean here would be a bug. On an expiry-driven component
                # the next due date comes off the stamp on the bottle, not from
                # the day you looked at it: check the extinguishers in April,
                # find a stamp saying August, and a plain "+365" reset hides the
                # reminder until four months after it lapsed. So the catalog,
                # not the item, decides which kind of record this writes.
                row["logs"] = "expiry" if node and comp[node].get("expires") \
                    else "record"
            if applies:
                row["appliesTo"] = applies
            # An audit found five items opening with "Трохи", "Перший", "У",
            # "Сильно" and the noun "Перевірка". A checklist is read at speed
            # in bad light: the lines have to share one grammatical mood, or
            # the eye has to parse instead of scan.
            #
            # The mood differs by list, though, and that is not sloppiness.
            # A lay-up list gives orders - "Зняти імпелер". A departure list
            # confirms states you are looking at - "Кінгстони відкрито" - and
            # rewriting those as orders ("Відкрити кінгстони") would tell the
            # skipper to do something he may already have done.
            head = iuk.split()[0].rstrip(",:")
            is_inf = head.endswith(("ти", "тись", "тися"))
            if mood == "infinitive" and not is_inf:
                print(f"    NOT AN INFINITIVE in {ic}: {head!r}")
                failed = True
            elif mood == "stative":
                if is_inf:
                    print(f"    ORDER IN A STATE LIST in {ic}: {head!r}")
                    failed = True
                elif not any(w.rstrip(",.;:—").endswith(("но", "то"))
                             for w in iuk.split()):
                    print(f"    NO CONFIRMED STATE in {ic}: {iuk!r}")
                    failed = True

            # An outboard has an impeller and raw water too, so `coolingAny`
            # alone let "draw antifreeze until it runs from the exhaust" reach
            # a trailered outboard boat. Raw-water cooling describes the
            # inboard's circuit; the rule must name the engine as well.
            if "coolingAny" in applies and "driveAny" not in applies:
                print(f"    coolingAny WITHOUT driveAny in {ic}")
                failed = True

            # On an expiry-driven component the date comes off a stamp, and a
            # stamp has a location. "Enter the date" is useless standing in
            # front of a bottle with three numbers on it.
            if logs and node and comp[node].get("expires") \
                    and "дат" not in iuk.lower():
                print(f"    EXPIRY ITEM DOES NOT ASK FOR A DATE: {ic}")
                failed = True

            rows.append(row)
            uk_terms[f"chk_{ic}"] = [iuk, verdict(ic, iuk)]
        out["checklists"].append(
            {"code": code, "season": season, "en": en, "items": rows})
        n_rec = sum(1 for r in rows if r.get("logs") == "record")
        n_exp = sum(1 for r in rows if r.get("logs") == "expiry")
        n_cond = sum(1 for r in rows if r.get("appliesTo"))
        print(f"{code:12} {len(rows):3} items   {n_rec:2} record   "
              f"{n_exp:2} ask for a new date   {n_cond:2} conditional")

    for path in (OUT,):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with io.open(path, "w", encoding="utf-8", newline="\n") as fh:
            json.dump(out, fh, ensure_ascii=False, indent=2)
            fh.write("\n")

    # Merge the uk strings into the existing glossary, keeping its order.
    glossary = json.load(io.open(UK, encoding="utf-8"))
    uk_comp = glossary["components"]
    glossary["checklists"] = uk_terms
    with io.open(UK, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(glossary, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    # Count it, never type it: this line said 77 while the table below it
    # listed 113. Same discipline as the
    # catalog: they ship only after a sailor has been through them.
    n_open = sum(1 for v in uk_terms.values() if v[1] == "check")
    head = [
        "# Чеклісти на перевірку — uk",
        "",
        (f"{n_open} з {len(uk_terms)} рядків чекають на прочитання. "
         "Питання до кожного одне:"
         if n_open else
         f"0 з {len(uk_terms)} — усі рядки вичитано моряком. "
         "Перевірка лишається тут, бо переписаний наказ повертається "
         "в неї автоматично:"),
        "**чи так це кажуть і чи так це роблять?**",
        "",
        "Другий стовпчик — вузол каталогу, до якого прив'язаний пункт.",
        "Третій — чи створює виконання запис у журналі й скидає інтервал.",
    ]
    for cl in out["checklists"]:
        head += [
            "",
            f"## {uk_terms['chk_' + cl['code']][0]} ({len(cl['items'])} пунктів)",
            "",
            "| Англійською | Наш варіант | Вузол (затверджений термін) | Журнал | Правка |",
            "|---|---|---|---|---|",
        ]
        for r in cl["items"]:
            if uk_terms["chk_" + r["code"]][1] != "check":
                continue
            # Printing the catalog's own approved term beside the item is
            # what makes wording drift visible: the checklist and the catalog
            # must call the same object the same thing.
            node = r.get("node")
            term = uk_comp.get(node, ["—"])[0] if node else "—"
            kind = {"record": "запис", "expiry": "**запис + дата**"}.get(
                r.get("logs"), "—")
            head.append(
                f"| {r['en']} | **{uk_terms['chk_' + r['code']][0]}** "
                f"| {term} | {kind} | |")
    out_md = os.path.join("catalog", "i18n", "uk-checklists-review.md")
    io.open(out_md, "w", encoding="utf-8", newline="\n").write(
        "\n".join(head) + "\n")
    print(f"review list -> {out_md}")

    nodes = Counter(r["node"] for cl in out["checklists"]
                    for r in cl["items"] if "node" in r)
    total = sum(len(cl["items"]) for cl in out["checklists"])
    linked = sum(nodes.values())
    print(f"\n{total} items, {linked} linked to {len(nodes)} of {len(codes)} components")
    dup = [n for n, k in nodes.items() if k > 1]
    if dup:
        print(f"referenced by both lists: {', '.join(sorted(dup))}")
    print(f"uk terms written to {UK} ({len(uk_terms)} keys, all 'check')")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
