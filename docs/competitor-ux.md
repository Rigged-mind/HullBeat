# Розбір інтерфейсів конкурентів

Джерело: 46 скріншотів, зняті вручну на Android 2026-09-05, у `pages/`-архівах.

⚠️ **Архіви підписані неправильно** (зміщені по колу). Фактичний вміст:

| Архів | Насправді | Версія |
|---|---|---|
| `Vessly.zip` | **Bavyn** | 1.0.0 |
| `Boat Maintenance Planner.zip` | **Vessly** | 1.4.0 |
| `YachtWave.zip` | **Boat Maintenance Planner** | 1.3.6 |

YachtWave зняти не вдалося — недоступний у регіоні Україна. Про нього — лише
з лістингу стору й сайту ([competitors.md](competitors.md)).

---

## 1. Bavyn 1.0.0 — мінімалізм, доведений до втрати сенсу

**Навігація:** 3 вкладки в плаваючій «пігулці» — `GARAGE · DUE · SETTINGS`.

**Онбординг: 3 кроки.** Крок 2 — «Where do you sail?» з вибором країни інспекції
(Spain ITB, UK BSS, Italy Revisione, Germany Bootszeugnis, France, +2), **автовизначення
за регіоном**. Пояснюють навіщо: «We use this to automatically calculate when your
inspection is due». Крок 3 — дозвіл на сповіщення, з виходом «Not now».

**Додавання човна — один екран:** чипи типу (Sailboat, Motorboat, Sportfish, RIB,
Dayboat, Tender, Trawler, Multihull) + чипи рушія (Inboard diesel, Inboard petrol,
Outboard, Pod/Saildrive, Sail only, Electric) + назва, довжина, рік, поточні мотогодини.
Підпис: «You'll log them manually each time you sail.»

**Картка човна:** фото, `48.0 h` + «View history», «No upcoming services»,
далі **ADD ENTRY — 5 кнопок**: Service · Refuel · Inspection · Season prepar… · Winterization.
Потім History.

**Чеклісти** гарні: «Season preparation», прогрес «0 of 14 items completed» зі смугою,
пункти + «Add custom item». Лексика точно збігається з нашою таксономією:
antifouling, sacrificial anodes, standing rigging, raw-water impeller,
fuel filter / water separator, seacocks, bilge pump auto/manual, VHF DSC test.

**Refuel** має калькулятор: літри / ціна за літр / разом — «Enter 2 values», рахує третє.

**Монетизація: 999,99 грн (~$24) разово.** Що за пейволом:
- другий човен
- **автоматичний розрахунок дати інспекції** («We compute when it's due based on country,
  length and age. Alerts at 60, 30 and 7 days out»)
- бекап у Google Drive
- сезонні чеклісти

На безкоштовному: «MANUAL DATE · No date set · **You log the date. No automatic
pre-alerts on Free.**»

**Найагресивніший хід:** на екрані бекапу безкоштовний стан підписаний
«Local backup active — **Only on this device · lose it and the data goes with it**».
Тобто вони самі створюють страх втрати даних і одразу продають ліки від нього.

**Чого в Bavyn немає взагалі:** імпорту, пошуку (жодного поля на жодному екрані),
дерева вузлів, історії по конкретному вузлу, паспорта вузла з номерами деталей,
інвентарю, фото до запису робіт.

**Дефекти:** «Save» у правому верхньому куті (недосяжно великим пальцем);
обрізаний підпис «Season prepar…»; сповіщення за замовчуванням `Off` попри окремий
крок онбордингу; головний екран для власника одного човна — це список з одного елемента
плюс банер Pro на пів екрана.

---

## 2. Vessly 1.4.0 — правильні гасла, перевантажена реалізація

**Стартовий екран:** «Your boat's logbook, always in your pocket. Log maintenance in
30 seconds, get reminded before things break, and show buyers a full service history
when you sell.» + «✓ **No account needed · your data stays on your phone**».
Кнопки: [Set up my boat] і «Restore from a backup».

⚠️ Тут же червоним висить «No backup found for this Google account yet.» —
**помилка на першому запуску, до будь-яких дій користувача**.

**Онбординг: 3 кроки.** Крок 2 — «Add your engine»: «Hour-based tasks — like oil changes
every 50 hours — need your engine's hour meter.» Поле «Engine · optional» з підказкою
`e.g. Volvo Penta D2-40`, поточні мотогодини, «+ Add second engine»,
і чудовий підпис «**Not sure? A rough number is fine — you can correct it anytime.**»
Два виходи: «No engine? Skip →» і «Skip — no engine on this boat».

**Фінал онбордингу:** «nvv's logbook is ready» + зведення (Boat / Engine / Backup)
+ «Add your first entry now — it takes 30 seconds».
⚠️ Але первинна кнопка — **[Home]**, а «Add first entry» — вторинна.
Дія має бути первинною; вони самі себе перемогли.

**Головний екран:** смуга «Sail · ● Backup not set up» → карусель MY BOATS з фото
→ велика темна картка **ENGINE HOURS · 89 h · tap to update** + «Engine hours & history»
→ NEXT UP → QUICK ACTIONS (Reading · Fuel · Expenses · Mechanical)
→ THIS SEASON (за Pro) → Boat details → **ALL MODULES: 15 плиток**.

Модулі: Tasks, Tickets, Mechanical, Equipment, Maintenance, Inventory, Checklists,
Cruise Logs, Readings, Expenses, Notes, Photos, Documents, Contacts, Crew.
Плюс «My Fleet — create a fleet or join with an invite code» і «Start your boat's CV».

⚠️ **Tasks / Tickets / Mechanical / Maintenance — чотири назви для перекривних понять.**
Це буквально форумна скарга «way too broad and inclusive that they become
overbearing to do simple data entry».

**Створення задачі — головна вада.** Двокроковий візард `1 TASK → 2 SCHEDULE`.
На першому кроці: Open/Completed, TITLE (з чипами-підказками «Oil change»,
«Impeller check», «Replace anodes»), TASK TYPE, PRIORITY (Low/Normal/High/Critical),
STATUS (Attention/In progress/On hold/Ordered), LOCATION, DETAIL,
MEASUREMENT TYPE (None/Battery Current/Battery Voltage/…),
ATTACHMENTS (Take photo / Choose photo / Attach document) → **[Next: Schedule]**.

Щоб записати заміну олії, треба пройти вісім полів і **другий екран**.
У нас це два дотики.

**Пошук** є, але лише всередині Tasks («Search tasks»). Глобального немає.

**Безкоштовний тариф: 3 човни** («Free plan · 1 of 3 boats used»).
За Pro — витрати за сезон і, судячи з банерів, ще частина.

**Найкраща ідея, яку варто забрати:** налаштування
«**Open camera automatically** — When adding a new entry, jump straight to the receipt camera».
Це камера-first ввід, майже те саме, до чого ми йшли через OCR чеків.

Ще в налаштуваннях: Language, Currency (USD), Dark theme, Help & feedback,
Share Vessly, Terms, Privacy, і **DANGER ZONE → Delete all data**.
Підпис у футері: «Vessly 1.4.0 · Made for boat owners».

---

## 3. Boat Maintenance Planner 1.3.6 — лідер Android, найзріліший

**Навігація: 4 плоскі вкладки** — `Overview · Parts · To-dos · Files`.
Угорі: гамбургер, перемикач човна «Orca ▾», шестерня. Найчистіша структура з трьох.

**Overview:** фото → назва → три показники в ряд **Engine hours · Miles · Parts**
→ чотири плитки дій: **Quick maintenance · Log hours/miles · Add part · Storage**
→ Upcoming Maintenance + «See all».

**Quick maintenance — найкращий екран у всій трійці.**
Заголовок «**What did you do?**», вільне поле «Maintenance title»
(підказка `Ex: Washed topside and rinsed deck`), і далі **сітка готових плиток,
у кожної підпис категорії**:

| Плитка | Категорія |
|---|---|
| Oil Change, Fuel Filter, Engine Service, Coolant Check | Mechanical |
| Antifouling, Anchor Service, Hull Polish, Sail Inspection | Hull & Exterior |
| Lifejacket Check | Safety |

Це швидше за ланцюжок «категорія → вузол → дія». Але далі — «Continue»,
тобто знову багатокроковість.

**Files** вимагає підключення **Dropbox**: «Encrypted end-to-end · Only a dedicated
folder accessed by our app · Available anytime, even when you're offshore».

**Storage period:** From / To / Storage location / Storage type — облік зимового
зберігання як окремої сутності. Розумно: стан човна впливає на те, що актуально.

**Меню:** Switch boat, Tutorial Videos, Instagram, Support, **Suppliers**,
**Ownership Transfers**, партнерство з Watersportverbond (нідерландська федерація
водних видів спорту). Тобто застосунок нідерландський і має інституційного партнера —
це і є їхня дистрибуція, якої немає в новачків.

⭐ **Ownership Transfers** — резельна вартість зроблена правильно: не PDF-досьє,
а **передача самого журналу новому власнику**. Це сильніше за все, що ми планували
в цьому напрямі.

**Дефекти:** заголовок екрана показує сирий маршрут «setup/nameSetup»;
обрізане «Storage locati…»; «Save» угорі праворуч і сірий, коли неактивний;
пошуку немає; потрібен акаунт.

---

## 4. Зведення: що є у кого

| | Bavyn | Vessly | BMP | Ми (план) |
|---|---|---|---|---|
| Вкладок у навігації | 3 | 5 + 15 модулів | 4 | 4 |
| Імпорт таблиці | ✗ | ✗ | ✗ | **✓** |
| Глобальний пошук | ✗ | лише задачі | ✗ | **✓ FTS4** |
| Дерево вузлів | ✗ | частково (Equipment) | частково (Parts) | **✓ 21 кат.** |
| Історія по вузлу | ✗ | ✗ | ✗ | **✓** |
| Паспорт вузла (номери деталей) | ✗ | ✗ | ✗ | **✓** |
| Фото до запису робіт | ✗ | ✓ | ? | ✓ |
| Галерея вузла за часом | ✗ | ✗ | ✗ | **✓** |
| Прогнозна дата | ✗ (лише інспекція, Pro) | ✗ | ✗ | **✓** |
| Дотиків на запис роботи | ~4 | **~10 + 2 екрани** | ~4 | **2** |
| Офлайн без акаунта | ✓ | ✓ | ✗ | ✓ |
| Безкоштовно човнів | 1 | **3** | ? | ~~1~~ **переглянути** |
| Ціна | 999,99 грн разово | Pro | Pro | $25–35 разово |

---

## 5. Що забрати (перевірено на живих екранах)

1. **Плитки готових робіт із підписом категорії** (BMP). Швидше за навігацію по дереву.
   Беремо в аркуш швидкого запису як другий ряд після «Зроблено».
2. **«Open camera automatically»** (Vessly) — опція одразу відкривати камеру чека
   при новому записі. Дешево, і точно в наш сценарій «фото як спосіб вводу».
3. **Чипи-підказки в полі назви** (Vessly, BMP): «Заміна олії», «Перевірка імпелера»,
   «Заміна анодів».
4. **Пояснювати, навіщо питаємо** (Bavyn: країна → авторозрахунок інспекції;
   Vessly: мотогодини → годинні регламенти). Онбординг без пояснень читається як анкета.
5. **«Не памʼятаю точно — приблизне число теж підійде»** (Vessly). Знімає паралізацію
   на першому екрані. У нас це вже є як `date_precision`, треба й для лічильника.
6. **Періодичне нагадування ввести мотогодини** (Bavyn: `Engine hours reminder · Monthly`).
   Прямо годує наш прогноз — без ряду показників він не працює. **Обовʼязково.**
7. **Облік зимового зберігання** (BMP: Storage period). Впливає на те, що показувати
   в «Зараз»: човен на березі — інші роботи актуальні.
8. **Передача власності** (BMP: Ownership Transfers). Сильніша форма резельної вартості,
   ніж PDF. У нас офлайн-версія: експорт повного «пакета судна» з передачею.
9. **Калькулятор пального** (Bavyn): 2 з 3 значень → рахує третє.
10. **«Danger zone → Delete all data»** (Vessly). Демонстрація контролю над даними.

## 6. Чого НЕ робити (кожен пункт бачив на екрані)

1. **«Save» у правому верхньому куті** (Bavyn, BMP). Для 6.7″ телефона однією рукою
   це недосяжно. Наша первинна дія — внизу, на всю ширину.
2. **Сітка з 15 модулів** (Vessly) і 12 плиток (YachtWave). Це меню, а не відповідь
   на «що мені робити».
3. **Чотири назви для одного поняття** (Tasks / Tickets / Mechanical / Maintenance).
   У нас: одна сутність «запис роботи» + теги типу роботи.
4. **Багатокроковий візард для однієї заміни олії** (Vessly: 8 полів + другий екран).
5. **Первинна кнопка не на дію** (Vessly: [Home] замість [Додати запис]).
6. **Червона помилка на першому запуску** (Vessly: «No backup found…»).
7. **Сирі маршрути в заголовках** (BMP: «setup/nameSetup») і **обрізані підписи**
   (Bavyn «Season prepar…», BMP «Storage locati…»).
   Це саме те, що покупець назвав «little errors indicative of shoddy coding».
8. **Нулі в бейджах** біля кожного модуля (Vessly). Шум.
9. **Кастрований безкоштовний тариф** (Bavyn: без автодат і без бекапу в Drive —
   тобто без того, заради чого застосунок і ставлять).
10. **Обовʼязковий сторонній акаунт для файлів** (BMP: Dropbox).

## 7. Наслідки для нашого плану

**7.1. Безкоштовний тариф треба переглянути.** «Free = 1 човен» гірше за Vessly (3)
і YachtWave (3 + 5 членів екіпажу). Кількість суден — слабкий пейвол: більшість
власників мають один човен, тому він нікого не конвертує й лише псує враження.
Пейвол має стояти на прогнозі, OCR, складному імпорті й QR-наліпках.

**7.2. Наші клини підтверджені на живих екранах.** У жодного з трьох немає:
імпорту, глобального пошуку, історії й паспорта на рівні вузла, галереї вузла за часом,
прогнозної дати. Запис роботи в найближчого конкурента — 4 дотики, у Vessly — близько
десяти й два екрани.

**7.3. Головна загроза — не фічі, а дистрибуція.** BMP має 10K встановлень не через
кращий продукт, а через партнерство з національною федерацією і навчальні відео.
Це підтверджує п.8 у [product-spec.md](product-spec.md).
