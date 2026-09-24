#!/usr/bin/env python3
"""Translate the passport field labels and keep them from drifting.

`specHints` are the rows of a component's passport - "Part number", "Micron",
"Torque spec". They lived only in catalog.json, in English, and the pipeline
never touched them: 98 components, 228 labels, none translated. It stayed
invisible while the component page was hardcoded to one node with three
hand-written Ukrainian rows.

152 of the 228 are unique and 120 of those appear once, so a shared dictionary
is the right shape: translate each distinct label once, apply it everywhere,
and fail the build when the catalog uses a label the dictionary has never seen.

Run:  python tools/make_spechints.py
Exit: 1 if any label in the catalog has no translation.
"""
import io
import json
import os
import sys

CATALOG = os.path.join("catalog", "catalog.json")
UK = os.path.join("catalog", "i18n", "uk.json")

# Short technical labels. Where an abbreviation is the thing people say
# (MMSI, CCA, CFM, Hex ID, DOT), it stays.
HINTS = {
    "Actuator type": "Тип приводу",
    "Alarm threshold (°C)": "Поріг сигналізації (°C)",
    "Anode part no": "Артикул анода",
    "Application date": "Дата нанесення",
    "Batten count": "Кількість лат",
    "Battery expiry": "Термін батареї",
    "Bearing kit": "Комплект підшипників",
    "Bearing part no": "Артикул підшипника",
    "Bellows part no": "Артикул сильфона",
    "Belt or chain": "Пас або ланцюг",
    "Belt part no": "Артикул паса",
    "Belt size": "Розмір паса",
    "Blades": "Лопаті",
    "Bolt count": "Кількість болтів",
    "Bottle size": "Обʼєм балона",
    "Bulb types": "Типи ламп",
    "Burner count": "Кількість конфорок",
    "CCA": "Пусковий струм (CCA)",
    "CFM": "Продуктивність (CFM)",
    "Cable part no": "Артикул троса",
    "Call sign": "Позивний",
    "Capacity": "Місткість",
    "Capacity (Ah)": "Ємність (А·год)",
    "Capacity (L)": "Обʼєм (л)",
    "Chemistry": "Тип хімії (AGM / гель / LiFePO₄)",
    "Cloth": "Тканина",
    "Coats": "Кількість шарів",
    "Colour": "Колір",
    "Compressor model": "Модель компресора",
    "Connector type": "Тип розʼєму",
    "Contents list": "Склад набору",
    "Controller model": "Модель контролера",
    "Coolant capacity (L)": "Обʼєм охолоджувальної рідини (л)",
    "Coolant spec": "Специфікація охолоджувальної рідини",
    "Cooler make": "Виробник охолоджувача",
    "Count": "Кількість",
    "Counts": "Кількість",
    "Cruising area": "Район плавання",
    "Cylinder size": "Обʼєм балончика",
    "DOT date": "Дата DOT",
    "Date": "Дата",
    "Date stamped on hose": "Дата, вибита на шлангу",
    "Diameter": "Діаметр",
    "Diaphragm kit part no": "Артикул комплекту мембрани",
    "Drive type": "Тип приводу",
    "Duct condition": "Стан рукава",
    "Earliest medicine expiry": "Найраніший термін препарату",
    "Element W": "Потужність елемента (Вт)",
    "Element part no": "Артикул елемента",
    "Exhaust gap": "Зазор випуску",
    "Expansion tank pressure": "Тиск у розширювальному бачку",
    "Expiry": "Термін дії",
    "Expiry date": "Дата закінчення",
    "Filter part no": "Артикул фільтра",
    "Fitted date": "Дата встановлення",
    "Fitted year": "Рік встановлення",
    "Flow (l/min)": "Продуктивність (л/хв)",
    "Fluid spec": "Специфікація рідини",
    "Fuel type": "Тип пального",
    "Fuse": "Запобіжник",
    "Fuse rating": "Номінал запобіжника",
    "Gap": "Зазор",
    "Gasket part no": "Артикул прокладки",
    "Gearcase oil": "Олива редуктора",
    "Gelcoat colour": "Колір гелькоуту",
    "Glow plug part no": "Артикул свічки розжарювання",
    "Grade": "Марка",
    "Grease type": "Тип мастила",
    "Gypsy size": "Розмір зірочки",
    "Handheld VHF": "Портативна УКХ-станція",
    "Height": "Висота",
    "Hex ID": "Hex ID",
    "Impeller part no": "Артикул імпелера",
    "Install date": "Дата встановлення",
    "Insurer": "Страховик",
    "Intake gap": "Зазор впуску",
    "Key size": "Розмір шпонки",
    "Last replaced": "Остання заміна",
    "Length": "Довжина",
    "Lengths": "Довжини",
    "Litres used": "Витрачено літрів",
    "MMSI": "MMSI",
    "Make": "Виробник",
    "Maker": "Виробник",
    "Material": "Матеріал",
    "Material (Zn / Al / Mg)": "Матеріал (Zn / Al / Mg)",
    "Membrane part no": "Артикул мембрани",
    "Micron": "Тонкість фільтрації (мкм)",
    "Model": "Модель",
    "Models": "Моделі",
    "Mount part no": "Артикул подушки",
    "Next check": "Наступна перевірка",
    "Next service due": "Наступний сервіс до",
    "Official number": "Офіційний номер",
    "Oil grade": "Вʼязкість оливи",
    "Oil quantity": "Обʼєм оливи",
    "Oil type": "Тип оливи",
    "Opening temp": "Температура відкриття",
    "Output (A)": "Струм віддачі (А)",
    "Packing size": "Розмір набивки",
    "Paint brand": "Бренд фарби",
    "Panel W": "Потужність панелі (Вт)",
    "Part number": "Артикул",
    "Pawl spring part no": "Артикул пружини собачки",
    "Pitch": "Крок гвинта",
    "Plug type": "Тип свічки",
    "Policy no": "Номер поліса",
    "Polish product": "Поліроль",
    "Pre-filter micron": "Тонкість префільтра (мкм)",
    "Pressure": "Тиск",
    "Pressure switch": "Реле тиску",
    "Probe part no": "Артикул датчика",
    "Propeller size": "Розмір гвинта",
    "Quantity": "Кількість",
    "Quantity (L)": "Обʼєм (л)",
    "Refrigerant": "Холодоагент",
    "Registered to": "Зареєстровано на",
    "Registry": "Порт реєстрації",
    "Rigger": "Такелажник",
    "Run time before start": "Час вентиляції до пуску",
    "Sailmaker": "Вітрильний майстер",
    "Scheme (ITB/BSS/Revisione/Bootszeugnis)":
        "Схема (ITB / BSS / Revisione / Bootszeugnis)",
    "Screen part no": "Артикул сітки",
    "Seal part numbers": "Артикули ущільнювачів",
    "Section": "Профіль",
    "Sensor expiry": "Термін сенсора",
    "Serial": "Серійний номер",
    "Service kit": "Ремкомплект",
    "Service kit part no": "Артикул ремкомплекту",
    "Shear pin part no": "Артикул зрізного штифта",
    "Sheave count": "Кількість шківів",
    "Signaling mirror": "Сигнальне дзеркало",
    "Silencer make": "Виробник глушника",
    "Size": "Розмір",
    "Size %": "Розмір (%)",
    "Sizes": "Розміри",
    "Socket size": "Розмір головки",
    "Software version": "Версія прошивки",
    "Spare aboard?": "Запасний на борту?",
    "Spares aboard": "Запасні на борту",
    "Stack insulation type": "Тип ізоляції стояка",
    "Surveyor": "Сюрвеєр",
    "Thread size": "Різьба",
    "Torque": "Момент затягування",
    "Torque spec": "Момент затягування",
    "Type": "Тип",
    "Type (A/B)": "Тип (A / B)",
    "Types": "Типи",
    "Water": "Вода",
    "Weight (kg)": "Вага (кг)",
    "Wire dia": "Діаметр троса",
    "Working pressure": "Робочий тиск",
    "Tube material": "Матеріал балонів (ПВХ / Hypalon)",
    "Number of chambers": "Кількість відсіків",
    "Year": "Рік",
}


def main():
    catalog = json.load(io.open(CATALOG, encoding="utf-8"))
    used = {}
    for cat in catalog["categories"]:
        for comp in cat["components"]:
            for hint in comp.get("specHints") or []:
                used.setdefault(hint, []).append(comp["code"])

    # A passport row that must exist wherever the component is a consumable
    # you have to order. This locks a regression: a one-shot script filtered
    # "Part number" and "Count" out of all seven anode nodes at once, and it
    # only surfaced when the component page started rendering real data.
    for cat in catalog["categories"]:
        for comp in cat["components"]:
            if "anode" not in comp["code"]:
                continue
            hints = comp.get("specHints") or []
            if "Part number" not in hints:
                print(f"    ANODE WITHOUT A PART NUMBER: {comp['code']}")
                used["__fail__"] = [comp["code"]]

    missing = sorted(set(used) - set(HINTS))
    extra = sorted(set(HINTS) - set(used))
    for label in missing:
        print(f"    NO TRANSLATION: {label!r}  (used by "
              f"{', '.join(used[label][:3])})")
    for label in extra:
        print(f"    WARN unused translation: {label!r}")

    glossary = json.load(io.open(UK, encoding="utf-8"))
    # Keep a verdict while the wording is the same, drop it when it changes:
    # a re-worded label has not been read in its new wording. Old bare-string
    # entries carry no verdict, so they start unread.
    previous = glossary.get("specHints") or {}

    def verdict(label):
        was = previous.get(label)
        if isinstance(was, list) and len(was) > 1 and was[0] == HINTS[label]:
            return was[1]
        return "check"

    glossary["specHints"] = {k: [HINTS[k], verdict(k)] for k in sorted(used)}
    with io.open(UK, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(glossary, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    # These go on the same review pile as the catalog terms and the
    # checklists. Count them, never type the number: the line under this one
    # said 152 while the file listed 154.
    n_open = sum(1 for v in glossary["specHints"].values() if v[1] == "check")
    out = os.path.join("catalog", "i18n", "uk-spechints-review.md")
    lines = [
        "# Підписи паспорта на перевірку — uk",
        "",
        (f"{n_open} з {len(used)} підписів чекають на прочитання "
         f"({sum(len(v) for v in used.values())} вживань на "
         f"{len({c for v in used.values() for c in v})} вузлах). "
         "Питання те саме: **чи так це називають?**"
         if n_open else
         f"0 з {len(used)} — усі підписи вичитано. Перевірка лишається тут, "
         "бо переписаний підпис повертається в неї автоматично."),
        "",
        "Третій стовпчик — вузол і його **затверджений** термін. Саме такий "
        "стовпчик у `uk-checklists-review.md` дав побачити вісім розходжень "
        "очима; тут його бракувало, і підпис `Coolant spec` лишався "
        "«охолоджувача» під вузлом «Охолоджувальна рідина».",
        "",
        "| Англійською | Наш варіант | Вузли | Правка |",
        "|---|---|---|---|",
    ]
    uk_comp = {k: (v[0] if isinstance(v, list) else v)
               for k, v in glossary["components"].items()}
    for label in sorted(used):
        if glossary["specHints"][label][1] != "check":
            continue
        where = ", ".join(f"{uk_comp.get(c, c)} (`{c}`)" for c in used[label][:3])
        if len(used[label]) > 3:
            where += f" (+{len(used[label]) - 3})"
        lines.append(f"| {label} | **{HINTS.get(label, '—')}** | {where} | |")
    io.open(out, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")

    print(f"catalog/i18n/uk.json           specHints: {len(used)} labels, "
          f"{sum(len(v) for v in used.values())} uses")
    print(f"review list -> {out}")
    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
