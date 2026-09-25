<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="design/logo/lockup-horizontal-dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="design/logo/lockup-horizontal.svg">
    <img src="design/logo/lockup-horizontal.svg" alt="HullBeat Logo" width="420">
  </picture>
</p>

<p align="center">
  <strong>Офлайн судновий журнал та планувальник технічного обслуговування для катерів і яхт</strong><br>
  <strong>Offline Boat Maintenance Logbook & Service Planner for Sail and Motor Vessels</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Offline--First-100%25-0E7C7B?style=flat-square" alt="Offline First" />
  <img src="https://img.shields.io/badge/Privacy-Zero%20Tracking-brightgreen?style=flat-square" alt="Privacy" />
</p>

<p align="center">
  <a href="#-українська">🇺🇦 Українська версія</a> &nbsp;•&nbsp; <a href="#-english">🇬🇧 English version</a>
</p>

---

# 🇺🇦 Українська

## 🌊 Що таке HullBeat простими словами?

Володіння катером чи яхтою — це не лише романтика моря, а й постійний догляд за десятками систем. На борту дуже легко втратити контроль:
* Чи пора міняти цинкові аноди на корпусі та сейлдвайві від корозії?
* Скільки років вантам і стоячому такелажу? Коли востаннє змащували палубні лебідки?
* Чи в нормі донно-забортні крани (кінгстони) та крильчатка помпи (імпелер)?
* Коли востаннє міняли оливу в двигуні та редукторі?
* Де саме на борту лежить запасний паливний фільтр і скільки їх залишилося?

**HullBeat** — це кишеньковий цифровий боцман і судновий журнал. Він об'єднує всі вузли вашого судна в одну наочну систему, автоматично розраховує регламенти за мотогодинами або днями і вчасно попереджає про все, що вимагає вашої уваги.

---

## 🛡️ Головні переваги

1. **100% Офлайн та абсолютна приватність:**
   * Працює будь-де: у відкритому морі, на якорі чи в марині без жодного сигналу зв'язку.
   * Жодних хмар, серверів і реєстрацій — усі дані, записи та фотографії зберігаються **виключно на вашому телефоні**.
   * У додатку повністю відсутній дозвіл на вихід в інтернет (`INTERNET permission`).

2. **Зручний імпорт та експорт історії обслуговування:**
   * **Гнучкий імпорт:** Швидке перенесення ваших наявних записів із таблиць Excel (.xlsx) або CSV. Розумний аналізатор (`ColumnMapper` / `CsvSniffer`) автоматично визначає стовпчики, формати дат і чистить дані.
   * **Друк та експорт:** Формування повного паспорта судна та сервісної історії у форматі **Excel** або **PDF** в один дотик — незамінно для страхових компаній, сюрвеєрів чи підтвердження вартості судна при продажу.

3. **Додавання зображень та фотофіксація:**
   * Можливість прикріпити фотографії до будь-якої сервісної роботи, огляду вузла чи дефекту.
   * Фотографування лічильника мотогодин для підтвердження показників.
   * Повнорозмірний перегляд фотографій із підтримкою жестів гортання (swipe), зумом, лічильником знімків та текстовими підписами.

---

## ✨ Ключові можливості

### 1. Головний розділ «Пульс» (Now)
«Пульс» — це оперативний командний місток вашого судна, де зібрано все найважливіше:
* **Статус судна в реальному часі:** миттєвий огляд стану — що прострочено, що підходить за строком найближчим часом, а що в нормі.
* **Пріоритет критичності:** системи першої необхідності (кінгстони, помпа, рівень оливи, кермовий привід) завжди автоматично піднімаються на самий верх списку.
* **Швидкий запис у 2 дотики:** кнопка дії прямо на картці (`Зроблено`, `Продовжено`, `Записати`, `Готово`). Розумний нижній аркуш запитує **лише ті поля, які потрібні** саме для цього вузла (для мотогодинних — поточні години лічильника; для документів, плоту та піротехніки — обов'язковий новий термін придатності).
* **Механізм обґрунтованого відкладення робіт (Defer):** опція *«Не можу зараз — відкласти»* (наприклад, «до весняного підйому на берег» або «на 30 днів»). Додаток тимчасово глушить нагадування на переході, зберігаючи факт прострочення для чесної історії. Скасування відкладення — в один клік.
* **Передрейсовий чекліст безпеки (Pre-departure):** швидка перевірка судна перед віддачею швартовів (закриття люків, трюмна вода, рівень палива, помпи, ходові вогні, рятувальні жилети).
* **Мультисудновість:** легке перемикання між різними човнами або дингі/тузиком у шапці екрана.

### 2. Повне охоплення всіх систем судна (не лише двигун)
* ⚓ **Корпус і підводна частина:** стан гелькоуту, огляд пера керма, донно-забортна арматура (кінгстони), антифоулінг (необростайка), контроль та заміна жертовних анодів (цинк/алюміній) для захисту від електрохімічної корозії, трюмні помпи та поплавкові датчики.
* ⛵ **Вітрильне озброєння та такелаж:** стоячий такелаж (ванти, штаги, талрепи), бігучий такелаж (фали, шкоти, топенанти), полотно та шви вітрил (грот, стаксель, генуя, генакер), регулярне розбирання та змащування палубних лебідок, закруток (furling) і блоків.
* 🪢 **Палуба та якірний пристрій:** брашпіль/якірна лебідка, маркування та знос якірного ланцюга, вертлюги, швартові качки, релінги, леєри та люки.
* 🔋 **Енергетика, електроніка та навігація:** стан сервісних і стартерних батарей (AGM/LiFePO4), сонячні контролери, інвертори, ходові та якірні вогні, картплотери та автопілот.
* 🚿 **Сантехніка та життєзабезпечення:** прісна вода, опріснювачі, танки сірих/чорних вод, помпи та клапани морських гальюнів.
* ⚙️ **Силова установка:** стаціонарні дизелі, підвісні двигуни, сейлдвайви (saildrive) з контролем стану ущільнювальної мембрани, валолінії, дейдвуди, заміна олив, фільтрів та імпелерів забортної води.

### 3. Судновий склад запасних частин
* Облік запасу деталей (фільтри, крильчатки, ремені, свічки, запобіжники).
* Точна прив'язка до суднових рундуків і схованок (*«Рундук лівого борту»*, *«Ахтерпік»*, *«Штурманський стіл»*).
* Попередження про критичний залишок деталей перед автономним переходом.

### 4. Зручний інтерфейс для моря
* **Цілі дотику від 56 dp:** інтерфейс оптимізовано під реальні морські умови — великі кнопки та проміжки, якими комфортно керувати мокрими руками або в цупких яхтових рукавичках під час хитавиці.
* **Тема для нічної навігації (Night Navigation):** спеціальна глибока червоно-бурштинова палітра на темному тлі, яка зберігає природну адаптацію очей рульового до темряви під час нічних вахт і не засліплює місток.
* **Режим «Яскраве сонце» (Sunlight Mode):** підвищений контраст 7:1 із чіткими межами для читання з екрана під прямими променями сонця на відкритій палубі.

---

## 🚀 Як зібрати та запустити проєкт

### Системні вимоги:
* **JDK:** 17 або 21
* **Android Studio:** Ladybug (2024.2.1) або новіша
* **Мінімальна версія Android:** Android 8.0 (API 26)
* **Цільова версія Android:** Android 15 (API 35)

### Швидкий старт:
```powershell
# 1. Клонувати репозиторій
git clone https://github.com/Rigged-mind/hull_beat.git
cd hull_beat

# 2. Збірка APK через термінал
.\gradlew.bat assembleDebug

# 3. Запуск модульних тестів
.\gradlew.bat testDebugUnitTest
```

---

<br>

# 🇬🇧 English

## 🌊 What is HullBeat in Plain Words?

Owning a powerboat, sailing yacht, or cruiser is rewarding, but keeping everything seaworthy is demanding. At sea, dozens of critical items compete for your attention:
* When were the sacrificial zinc anodes on the hull and saildrive last inspected?
* What is the age of the standing rigging, stays, and turnbuckles? When were the deck winches last serviced?
* Are the seacocks, through-hulls, and the raw-water pump impeller in reliable condition?
* When was the engine and transmission oil last changed?
* Exactly which locker holds the spare fuel filter, and how many are left?

**HullBeat** is an offline digital boat logbook and equipment maintenance manager. It unifies all vessel components into a clear, actionable dashboard, computes service intervals by engine hours or calendar dates, and ensures you are ready to cast off with peace of mind.

---

## 🛡️ Key Advantages

1. **100% Offline & Absolute Privacy:**
   * Works offshore, at anchor, or in remote marinas with zero cellular connectivity.
   * Zero clouds, zero tracking, no accounts — your vessel data and photos stay **exclusively on your physical device**.
   * Completely lacks the Android `INTERNET` permission.

2. **Seamless Maintenance Import & Export:**
   * **Intelligent Import:** Easily migrate records from your existing Excel (.xlsx) or CSV spreadsheets. The built-in `ColumnMapper` and `CsvSniffer` automatically detect headers, clean data, and parse date formats.
   * **PDF & Excel Export:** Generate comprehensive vessel passports and dated maintenance histories in **Excel** or **PDF** format with one tap — essential for insurance surveys, mechanics, or resale value verification.

3. **Full Photo Documentation & Inspection:**
   * Attach high-resolution photos to any service record, component card, or defect report.
   * Photo logging for engine hour meters to verify readings.
   * Full-screen gallery viewer featuring intuitive swipe gestures, zoom, photo counter, and custom captions.

---

## ✨ Comprehensive Features

### 1. The "Pulse" (Now) Command Center
The core dashboard designed for immediate situational awareness:
* **Live Vessel Readiness:** Instant status showing overdue tasks, upcoming maintenance, and systems in good standing.
* **Safety Ranking:** Mission-critical items (raw-water impellers, seacocks, engine oil, bilge pumps) automatically float to the top of the list.
* **Quick 2-Tap Logging:** Tap action buttons directly on the card (`Done`, `Renewed`, `Record`, `Check`). The dynamic bottom sheet requests **only the specific fields needed** to clear the item (e.g., hour reading for engine items; new mandatory expiry date for life rafts, pyrotechnics, and documentation).
* **Legitimate Deferrals (Defer):** "Cannot do now — defer" option (e.g., defer until winter haul-out or for 30 days). Suppresses nagging alerts during a voyage while retaining the overdue fact in the permanent log.
* **Pre-Departure Safety Checklist:** Quick inspection protocol before slipping lines (hatches secured, bilge status, fuel level, bilge pumps verified, navigation lights tested, life jackets accessible).
* **Multi-Vessel Support:** Switch between your cruising boat, tender, or dinghy in seconds.

### 2. Complete Vessel Systems Coverage (Far Beyond Engines)
* ⚓ **Hull & Underwater Gear:** Gelcoat inspections, rudder stock and bearing play, seacocks, through-hulls, antifouling condition, cathodic protection (zinc/aluminum sacrificial anodes), bilge pumps, and float switches.
* ⛵ **Sails & Rigging:** Standing rigging (shrouds, stays, turnbuckles, swage fittings), running rigging (halyards, sheets, outhauls), sail cloth and seam inspections (mainsail, jib, genoa, spinnaker), deck winch servicing and pawl lubrication, furling gear.
* 🪢 **Deck & Ground Tackle:** Windlass maintenance, anchor chain wear and marking, swivels, mooring cleats, stanchions, lifelines, and deck hatches.
* 🔋 **Electrical & Navigation:** House and starter battery banks (AGM / LiFePO4), solar charge controllers, inverters, navigation lights, chartplotters, and autopilots.
* 🚿 **Plumbing & Domestic Systems:** Fresh water pressure pumps, watermakers, holding tanks, and marine toilet (head) valve servicing.
* ⚙️ **Propulsion Systems:** Inboard diesels, outboards, saildrives (diaphragm seal monitoring), shafts, stuffing boxes/dripless seals, fluid changes, and raw-water impellers.

### 3. On-board Spare Parts Inventory
* Track spare parts stock (fuel/oil filters, spare impellers, belts, fuses, bulbs).
* Precise locker assignments aboard (*"Port salon locker"*, *"Lazarette"*, *"Nav station drawer"*).
* Minimum threshold alerts before heading out on extended passages.

### 4. Marine-Optimized User Interface
* **Touch Targets ≥ 56 dp:** Engineered for challenging conditions at sea — oversized touch targets and generous spacing ensure reliable operation with wet hands or heavy sailing gloves while pitching.
* **Night Navigation Theme:** Deep red-amber palette on an ultra-dark background designed to preserve night vision at the helm during night passages.
* **Sunlight Mode:** High-contrast 7:1 ratio with reinforced borders for direct midday sun reading on an open cockpit display.

---

## 🚀 Build & Run

```bash
# Clone repository
git clone https://github.com/Rigged-mind/hull_beat.git
cd hull_beat

# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📁 Repository Structure

```
hull_beat/
├── app/                  # Main Android application (Jetpack Compose, Room, Kotlin)
│   ├── src/main/java/    # Clean architecture: UI, ViewModels, Database, Importer/Exporter
│   └── src/test/         # Unit tests covering engines, calculations, and parsers
├── catalog/              # Preloaded marine component ontology, checklists, and synonyms
├── design/               # Design system: vector logos, palette tokens, store assets
├── docs/                 # Complete architecture, UI guidelines, and engineering specs
├── tools/                # Python verification suites, linters, and palette generators
├── template/             # Canonical Excel reference template (.xlsx)
└── prototype/            # Interactive HTML5 prototype for offline UI testing
```

---

## 📄 Author & License

* **Author:** Vladyslav Kravchenko ([@Rigged-mind](https://github.com/Rigged-mind))
* **Email:** [riggedmind2049@gmail.com](mailto:riggedmind2049@gmail.com)
