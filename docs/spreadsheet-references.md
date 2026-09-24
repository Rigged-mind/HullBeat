# Розбір реальних таблиць обслуговування

Дата: 2026-09-05. Дивився на скріншоти самих таблиць, не на описи.
Це доповнення до [forum-evidence.md](forum-evidence.md) п.4, де були лише словесні
описи layout'ів від власників.

## 1. MV Dirona — найцитованіший референс, і найзріліший

`mvdirona.com/2015/03/maintenance-log/`. Nordhavn 52, кругосвітка.
Дві книги: `DironaMaintenanceSchedule.xlsx` і `DironaExpirationSchedule.xlsx`.

### 1.1. Лічильники — окремим блоком угорі, і їх ШІСТЬ

```
Hours   Main              5,208
        Wing                654
        Generator         3,910
        Water Maker         690
        Scuba compressor     58
        Honda BF40d         371
```

Підтверджує наше рішення привʼязувати `Meter` до вузла, а не до судна.
Але ширше, ніж я закладав: лічильник має не лише двигун — **опріснювач,
компресор і генератор теж**. У каталозі це треба дозволити будь-якому вузлу,
а не лише `engine_main` і `genset`.

### 1.2. Колонки розділу

```
Maintenance item | Months due | Hours due | Done date | Done hours |
Months left | Hours left | Acknowledge
```

Місяці **й** мотогодини поруч, «що настане першим» — рівно наша модель.
Прострочення показане в дужках за бухгалтерською традицією: `(37)` = −37 місяців.

### 1.3. ⭐ Поріг попередження задається ПО КОЖНІЙ СЕКЦІЇ, не глобально

```
Main engine              Warning threshold months 1 | Warning threshold hours 50
Wing engine              Warning threshold months 1 | Warning threshold hours 15
Hydraulics & Thrusters   Warning threshold months 1 | Warning threshold hours 40
General                  Warning threshold months 1 | (годин немає взагалі)
```

У нас `SOON_WINDOW_DAYS = 30` — одна константа на все.
Це неправильно: 50 мотогодин запасу на головному двигуні й 15 на допоміжному —
різні речі, бо різний наробіток.

Ще деталь: секція «General» **не має колонок годин узагалі**. Таблиця міняє набір
колонок залежно від секції. Наш імпортер має це пережити.

### 1.4. ⭐⭐ «Acknowledge» — механізм відкладання. Головна знахідка

Дві різні нотації в одній колонці:

| Запис | Значення | Як показано |
|---|---|---|
| `ACK:Yard` | визнано, відкладено до верфі | назва пункту стає **зеленою**, попри −37 місяців |
| `M:12` | продовжити на 12 місяців | зсуває наступний термін |
| `M:1` | продовжити на 1 місяць | те саме |

Кольори назви пункту: чорний — не настало, помаранчевий — у вікні попередження,
**червоний — прострочено**, **зелений — визнано й відкладено**.

**Цього в нас немає, і це критично.** Власник, який знає, що аноди прострочені,
але свідомо чекає підйому в жовтні, мусить мати спосіб прибрати пункт із червоного
**не збрехавши, що зробив**. Інакше екран «Зараз» назавжди лишається червоним,
людина перестає йому вірити — і застосунок помирає рівно так, як помирають усі інші.

Дуже показово: у Dirona є пункт із простроченням **−37 місяців**, який позначено
`ACK:Yard` і він спокійно живе зеленим. Три роки. Це нормальна експлуатація, а не
недбалість, і софт мусить це вміти.

### 1.5. ⭐ Аркуш термінів згрупований за ЧАСОМ ОФОРМЛЕННЯ, не за категорією

`DironaExpirationSchedule.xlsx`:

```
Quick turnaround         Warning threshold months 1
   Iridium minutes, BGAN units, UPS mailbox, airline miles, AT&T Prepaid
Multi-month turnaround   Warning threshold months 6
   CG documentation
Long turnaround          Warning threshold months 8
   Passports, Drivers licences
```

Логіка: **попереджати треба настільки заздалегідь, скільки триває оформлення**,
а не за єдиним правилом. Паспорт — 8 місяців, передплачена SIM — 1 місяць.

Для нас це означає: у `Document` потрібне поле «час на оформлення» (lead time),
а не глобальні 30 днів. Дефолти беруться з каталогу: сервіс плота бронюють
за місяці, страховку — за тижні, реєстрацію — за пів року.

## 2. The Vanabond Tales — 18 аркушів, дашборд угорі

`thevanabondtales.com/sailboat-maintenance-log-spreadsheet/`

Аркуші: Dashboard/KPI, Maintenance Schedules, Engine Hours Log, Engine Maintenance Log,
Electrical Systems, Battery Health & Usage, Fuel Tank, Water Tank, Watermaker,
Rigging & Shroud Tensions, Sails & Canvas, **Thru-Hull Locations**, Safety Equipment,
Upgrades & Works, Commissioning Checklist, Decommissioning Checklist,
Inventory «What's Where», Basic Sail Log.

На скріншоті видно колонки:
`Category | Service Frequency | Last Service Date | Next Service Date | Eng Hours Since Last Service`

Два спостереження:

- **Категорія і частота — кольорові чипи-випадайки**, не вільний текст.
  Ми це вже робимо через `_meta`-списки.
- **«Eng Hours Since Last Service»** — показано **скільки минуло**, а не скільки
  лишилось. У нас лише «залишилось». Обидва числа корисні: минуло — це факт,
  лишилось — це прогноз.
- Окремий аркуш **Thru-Hull Locations** — карта кингстонів. Перегукується з нашими
  локаціями та QR, але це саме безпекова схема: де що перекрити, коли тече.

## 3. Astrolabe Sailing «The Ultimate Boating Spreadsheet»

`astrolabesailing.com`, Viki Moore, Нова Зеландія. **21 аркуш**: Boat Maintenance,
Boat Details, Spare Parts, Personal Inventory, Passage Plan, Passage Log, Sight Log,
Deviation Table, Personal Details, Annual Spend, Income, Annual Budget, Power Budget,
Contact Details, Planning Calendar, Provisioning, Meal Plan, First Aid, SOPs,
Rescue Me, Safety Diagram.

Найважливіше — **чого там немає**: авторка прямо визнає, що бракує умовного
форматування, яке «turns red when things become overdue».

Тобто найширша з відомих таблиць — це **архів, а не планувальник**. Підтверджує,
що обсяг не є цінністю сам по собі; цінність — у тому, щоб щось загорілося вчасно.
Наш шаблон із світлофором на цьому тлі виграє, попри вшестеро менше аркушів.

Побічно: більшість «зайвих» аркушів (Passage Plan, Power Budget, Provisioning,
Meal Plan) — це життя на борту, а не обслуговування. Свідомо не наша тема.

## 4. Що змінюємо

| Знахідка | Джерело | Дія |
|---|---|---|
| **Відкладання (ACK)** без брехні «зроблено» | Dirona 1.4 | `ServiceSchedule.deferredUntil` + `deferReason`; новий стан `DEFERRED` |
| **Продовження на N місяців** (`M:12`) | Dirona 1.4 | те саме поле, задається як «відкласти до дати» |
| **Поріг попередження на рівні вузла/категорії** | Dirona 1.3 | `ServiceSchedule.soonWindowDays` і `soonWindowHours`, дефолт із каталогу |
| **Lead time для документів** | Dirona 1.5 | `Document.leadTimeDays`, дефолт із каталогу за типом документа |
| **Лічильник у будь-якого вузла** | Dirona 1.1 | опріснювач, компресор — не лише двигуни |
| **Показувати «минуло», а не лише «лишилось»** | Vanabond | друге число на картці вузла |
| **Секція без колонок годин** | Dirona 1.3 | імпортер не має падати на різному наборі колонок між секціями |
| Обсяг ≠ цінність | Astrolabe | не роздувати шаблон; світлофор важливіший за 21 аркуш |

## 5. Джерела

- mvdirona.com/2015/03/maintenance-log/ (+ два .xlsx у відкритому доступі)
- thevanabondtales.com/sailboat-maintenance-log-spreadsheet/
- astrolabesailing.com/2017/11/27/the-ultimate-boating-spreadsheet/
- ourpositivelatitude.com/captains-log-excel-template/ (Deck Log, Marina/Anchorage, Fuel, Pumpout)
- starstuffbooks.com — Vessel Maintenance Log, 22 вкладки за системами
- forums.sailboatowners.com — «Simple Engine Maintenance Log»
- trawlerforum.com/threads/using-spreadsheets-for-a-logbook.54108/
