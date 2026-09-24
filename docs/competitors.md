# Конкурентний аналіз: застосунки обслуговування човнів

Дата збору: 2026-09-04. Джерела: Google Play, App Store, iTunes Search API, сайти вендорів,
форумні треди (частина недоступна через Cloudflare — позначено в п.6).

## 0. Методологічне попередження

Запити «best boat maintenance software 2026» видають gitnux.org, zipdo.co, worldmetrics.org —
це SEO-фарми з вигаданими оцінками («Boatrax 9.7/10»). Плюс owlmar.com/blog та
yachtwyse.com/blog — це блоги самих конкурентів, які «оцінюють» ринок на свою користь.
**Жодне число звідти не використано нижче.** Усе, що тут є, — з Play/App Store, API Apple
або сайтів вендорів.

## 1. Масштаб Android-ринку (платформа проєкту)

| Застосунок | Розробник | Встановлень (Play) | Примітка |
|---|---|---|---|
| Nebo | Nebo Global | **50 000+** | воєдж-лог + соцмережа, НЕ обслуговування |
| Boat Maintenance Planner | Vesselscan | **10 000+** | лідер категорії обслуговування |
| YachtWave | Durban Equity Holdings II | **10 000+** | безкоштовний назавжди, найкращі оцінки в iOS |
| Boat Fix Pro | BOAT FIX, INC. | 1 000+ | компаньйон до GPS-трекера, не конкурент |
| BoatMatey | Donkey Labs | 50+ | |
| Vessly | GenX Development Studio | 10+ | оновлено 2026-09-02 |
| Boat Maintenance | IT APP Solutions | 10+ | |
| Bavyn | SRG Consulting | 5+ | |
| VesselMinder | Peralta Group Inc | 5+ | |

**Висновки:**

1. Лідер категорії обслуговування має ~10K встановлень. Це вся вершина ринку.
2. Майже жоден застосунок не має достатньо оцінок, щоб Play показав зірки.
3. Два найстаріші iOS-інкумбенти **не мають Android-версії взагалі**:
   Boating Suite (з 2010, «#1 boating logbook on the App Store», лише iPhone/iPad/Mac)
   і ShipShape. Індустріальні інструменти (Vessel Vanguard, Boatrax) — web/iOS-first.
4. Отже: Android недообслужений, але й сам ринок малий. **Ніхто не переміг.**
   Вузьке горло — дистрибуція, а не фічі.

### Масштаб в iOS (iTunes Search API, US)

| Застосунок | Ціна | Оцінка | К-ть оцінок | Реліз |
|---|---|---|---|---|
| BoatUS Weather & Tides | Free | 4.79 | 57 947 | 2017 |
| YachtWave | Free | 4.83 | **164** | 2023 |
| Wave Marine (сервіси на замовлення) | Free | 4.79 | 19 | 2022 |
| Boating Logbook: Skipper | Free+IAP | 4.6 | 36 | — |
| Vessel Vanguard | Free | **3.67** | 9 | 2023 |
| TreMate | Free | **3.33** | 6 | 2023 |
| Marina Maintenance Log | Free | 4.5 | 4 | 2025 |
| Boat Butler | Free | 3.0 | 2 | 2022 |
| Ready4Sea | Free+IAP | 5.0 | 2 | — |
| Boat Checked | Free | — | 0 | 2025 |
| Boat Maintenance (IT Consult) | Free | — | 0 | 2026 |
| Boat Maintenance Planner (R. van der plas) | Free | — | 0 | 2024 |

BoatUS наведено лише для масштабу — це не конкурент. Максимум у категорії обслуговування —
**164 оцінки**. Різниця з BoatUS (57 947) показує, наскільки ніша вужча за
«загальнобоатерські» застосунки.

## 2. КРИТИЧНО: наше позиціювання вже зайняте — двічі, за останні тижні

Позиціювання «без підписки + свій Google Drive + PDF при продажу» вже реалізоване.

### Bavyn (SRG Consulting) — найближчий конкурент

Дослівно з опису в Play:

- «No account, no subscription, one purchase»
- «for sailors and motorboat owners **who used to track engine hours and boat maintenance in Excel**»
- «Your data lives in your own Google Drive. **We never see it.** No analytics tracking,
  no email required. **Stop using Bavyn and you keep the backup.**»
- «hours or months, **whichever comes first**» — та сама логіка подвійного інтервалу
- Мультикраїнні інспекції: ITB (Іспанія), Bootszeugnis (Німеччина), Revisione (Італія), BSS (UK)
- Сезонні чеклісти за типом човна, редаговані
- PDF-експорт повного журналу («handy when selling the boat»)
- Цільова аудиторія: вітрильники й моторні 5–15 м
- Free: 1 судно, лише ручні нагадування. Pro: PDF-експорт, бекап, безліч суден
- 6 мов, iOS + Android

### Vessly (GenX Development Studio) — оновлено 2 дні тому

- «No account, no login: set up your boat in 2 minutes»
- «Works offline: log jobs at the dock, on the hard, or offshore»
- «Your data stays on your device, with optional backup to **your own Google Drive**
  (private, app-only folder)»
- «Export your full data as CSV or PDF anytime. **Your data is never locked in**»
- «Boat CV» PDF: машинерія, обладнання, витрати з чеками, логбук, показники, чеклісти,
  документи, інвентар, фото, задачі, проблеми, контакти
- Нагадування за мотогодинами + сезонні, прострочені підсвічено
- Розбивка витрат за категоріями й роками
- Data safety: «No data collected», «No data shared with third parties»

### VesselMinder (Peralta Group)

- «offline-first, works without a cell signal», «Built for the dock, not the desk»
- Free forever: 1 човен, безліч записів, трипи, витрати, документи, **до 5 людей доступу**
- Pro **$49/рік**: кілька човнів, більше сховища, PDF-експорт, нагадування
- Team Access з правами: «Your mechanic can log their own work and upload photos
  without touching anything else»
- «Receipt Scanning — Photograph invoices... tagged, and **searchable forever**»
- Резельна вартість: «you don't hand over a pile of faded paperwork — **you send a link**»

**Наслідок:** «без підписки, свій Drive, PDF для продажу» — це вже не диференціатор,
а **вхідний квиток**. Диференціюватися доведеться на рівні виконання, а не позиціювання.

## 3. Що люди дослівно критикують

### 3.1. Обов'язковий акаунт = видалення

**TreMate, 1★, «Required account - Instant delete»** (Steve in Kitsap, 2024-08-13):

> «You are required to start an account... STILL required to give them your name and information»

**Vessel Vanguard, «No way to set up an account»**:

> «Downloaded the app. Opened the application. Then it asked for you to login with your account
> information. As a new user, it does not allow you to set up an account.»

Ще один відгук там же — просто «No way to sign up». Плюс «Terrible company»:

> «Nobody here cares enough to bother replying to you»

Це B2B-інструмент, ворожий до приватного власника. Оцінка 3.67 при 9 голосах.

### 3.2. Немає офлайну = сміття

**TreMate, 2★, «It's OK»** (Paul…M):

> «It requires data to work... for me not worth having or using. **I want an off line option.**»

### 3.3. Незрозумілий інтерфейс

**TreMate, 1★, «Clumsy UI, usage unclear, acct required»** (scott.paul, 2025-01-29):

> «The UI is clumsy and awkward... unclear exactly how to use the app to track maintenance schedules»

Також по інших продуктах: Quartermaster — інтерфейс не масштабується на телефон,
потрібно постійно зумити й скролити; YMP — застарілий вигляд, мобільний застосунок
«feels like an afterthought»; Deep Blue — «way too difficult to set up».

### 3.4. Пошук не працює — навіть у найкращому застосунку

**YachtWave** (4.83★, 164 оцінки — лідер за якістю):

> «just another list database», «**fails on fundamental search functionality**»

### 3.5. Планування — найслабший модуль у всіх

**YachtWave:** «maintenance scheduling and tracking still needs improvement and isn't very intuitive».
Оператор флоту з 5 суден: «the closest to a comprehensive small fleet management solution
I have found», **але** «scheduling needs improvement». Користувачі застосовують «little hacks»,
щоб обійти брак функціоналу.

**TheBoatApp:** облік обслуговування «extremely basic», немає ні розумних нагадувань,
ні відліку за мотогодинами, ні прогнозування — просто списки.

### 3.6. Кейс ShipShape: втрата даних, яка здійснилася

Тред «Warning for Anyone Using Shipshape» (TrawlerForum, **березень 2018** — тред старий,
це не свіжа подія). Автор — angus99, Defever 44.

Що сталося (дослівно):

> «the current rev is no longer supported. The developer says it will continue to work until
> Apple changes its OS, rendering the app useless. The new version of Shipshape (a $9 annual
> subscription) **is not backwards compatible** according to the vendor's FAQ, so data cannot
> be migrated. To avoid losing all my data, **I would have to print it out and manually
> re-enter it** in the new app... I can't think of a better way to say "screw you" to customers.»

Через 4 дні автор підтвердив: **експорту не існує взагалі.**

> «Turns out there is **no way to export the data** from the old version of Shipshape, so I'll
> have to manually convert it to the new one. The developer says that creating an API to back up
> the older version in Dropbox **would cost more than the revenue they project to earn from the app**.»

**Але фінал важливо передати чесно:** вендор відреагував добре — вибачився, дав Pro-версію
безкоштовно, а Pro **за архітектурою бекапиться в Dropbox**. Автор лишився користувачем
і назвав Pro «a very robust set of tools». Тобто це не історія про негідника, а історія про
економіку ніші. Як маркетингова зброя вона слабша, ніж здається.

**Найцінніше з цього треду — цитата вендора про ринок:**

> «They noted **there's not big bucks in boat maintenance software** and said Shipshape is
> actually **a net money loser for them**. They're motivated to produce it, they say, because
> the boss has a passion for sailing.»

Супутні скарги на ShipShape від інших учасників:

> dhays: «I don't think it has a great interface. It is not intuitive at all and **you have to go
> between a lot of tabs to get the info that you want.** Just last weekend I was considering again
> going to just an Excel spreadsheet. **Then it would not be platform dependent.**»

> mcboatface: «I paid $30 for it about 18 months ago, but still use it every day.
> **A bit buggy at times but the best of what I could find out there.**»

Контраргумент проти таблиць — від того самого автора треду:

> angus99: «it's far from perfect, but **still easier than Excel for me. I will never get the hours
> of my life back I spent playing with Excel with little to show for it.**»

Ціни ShipShape: $39 у власність АБО $9/рік; ShipShape Pro $39.99 + пакети додаткових
човнів $29.99 / $129.99 / $239.99 за 1 / 5 / 10. Оцінка учасника треду:
«Both are expensive for apps, but **less than a rounding error in boat ownership**».
Вендор змінив назву на TicketyBoo (те саме Intelligent Maintenance LLC).

## 4. Цінова карта

| Продукт | Модель | Ціна |
|---|---|---|
| YachtWave | free forever (personal) + Fleet-тарифи | $0 |
| VesselTwin, VesselVault | free | $0 |
| Bavyn | **разова покупка** | не опубліковано; free = 1 судно |
| Ready4Sea | IAP-тарифи | $39.99 / $79.99 |
| BoatMatey | підписка | $4.99/міс, $39.99/рік |
| VesselMinder | підписка | $49/рік |
| Skipper (логбук) | підписка + lifetime | $7.99/тижд, $35.99/рік, **$124.99 lifetime** |
| ShipShape | разова / підписка | $39 або $9/рік; Pro $39.99 |
| Seapilog (чартер) | підписка/судно | від $19/міс |
| Boatrax | підписка/судно | $19.99/міс базовий, $29.99/міс Pro → **$240–360/рік** |
| Vessel Ready | підписка | $29/міс або $290/рік |
| **Plan M8** | підписка | **$40/міс**, збито до **$22/міс** проханням про «recreational boater discount» |
| **Vessel Vanguard** | підписка | **$1 300/рік** (за оцінкою користувача, орієнтований на флоти) |
| **Yacht Manager** | разова, Windows-програма | **$50** |
| IDEA / Deep Blue / Seahub (суперяхти) | ліцензія | **$10 000+** |

Розкид у 300 разів: $0 → $10 000+. Приватний власник 5–15 м платить $0–50 на рік
і чинить опір підпискам. Але той, хто справді шукав інструмент і порівняв дюжину,
заплатив **$22–40/міс** і був задоволений (див. п.8.1) — тобто платоспроможний сегмент
існує, він просто вузький.

## 5. Підтверджені прогалини, які не закриває НІХТО

1. **Імпорт існуючих даних.** ⚠️ **ВИПРАВЛЕНО 2026-09-05.** Початкове твердження
   «імпорту немає ні в кого» — **неточне**.

   `yachtwave.com/owners` прямо заявляє **CSV-імпорт** для задач, обслуговування
   й інвентарю. На скріншоті лістингу видно плитку «Import / Export» у головному меню.
   При цьому в описі Play імпорт **не згадується жодним словом** — тому пошуком
   він і не знаходився.

   Що лишається справжньою прогалиною: імпорт у всіх — **плоский CSV**.
   Немає доказів, що хтось тримає реальні layout'и власників — матрицю з роками
   в шапці, багатовкладкові книги, злиття «файл на рік»
   ([forum-evidence.md](forum-evidence.md), п.4). А саме вони й переважають.

   Клин переформульовано: не «ми маємо імпорт, а вони ні», а
   **«наш імпорт розуміє те, як таблиці справді виглядають»**.

2. **Пошук, що працює.** Критикують навіть найкращий застосунок категорії.
   Ніхто не заявляє повнотекстовий пошук по всьому (записи, підписи фото, запчастини, чеки)
   як headline-фічу.

3. **Планування зроблене правильно.** Універсально найслабший модуль **у мобільних**.
   Мультиодиничні інтервали серед мобільних заявляє лише Bavyn.
   ⚠️ Поправка за форумними даними: на десктопі це вирішено давно — **Yacht Manager**
   ($50, Windows) має «scheduled maintenance entries can be set to equipment hours, time,
   **or both**» і синхронізацію мотогодин із лог-софту. Тобто фіча не нова як ідея,
   вона просто відсутня в мобільних. Так само «світлофор» не є новинкою для досвідчених
   власників: умовне форматування Excel (жовтий/червоний при перевищенні годин або дати)
   вони вже застосовують — див. п.8.4.

4. **Інвентар із фізичною локацією + друковані QR-наліпки.** YachtWave має локації в інвентарі,
   але друкованих QR-наліпок на ящики немає ні в кого. При тому власники на форумах буквально
   тримають нумеровані ящики й шукають вміст у таблиці.

5. **Регуляторні дедлайни за країною.** Лише Bavyn (ITB / Bootszeugnis / Revisione / BSS).
   Для ЄС це сильно.

6. **OCR лічильника мотогодин.** Сканування чеків є (YachtWave, VesselMinder,
   Vessly «snap the receipt»). Розпізнавання цифр із лічильника мотогодин по фото — немає ні в кого.

7. **Запис у два дотики прямо зі сповіщення.** Не заявляє ніхто.

8. **Приймання-передача судна з фотофіксацією пошкоджень.** Лише індустріальні
   чартерні інструменти, дорого.

## 6. Джерела, недоступні автоматично (Cloudflare 403)

**СТАТУС: закрито.** Сторінки збережено вручну в `pages/` (2026-09-04), текст витягнуто
скриптом, результати — у розділі 8. Виняток: morganscloud за платною підпискою
(доступний лише вступ), YachtForums — витягнуто. Перелік для трасування:

- thehulltruth.com/maintenance-detailing/1340159-maintenance-log-program-app.html
- thehulltruth.com/maintenance-detailing/1312528-boat-maintenance-app-experience.html
- thehulltruth.com/boating-forum/639779-maintenance-log-spreadsheet.html
- trawlerforum.com/threads/yacht-maintenance-app-or-software.66811/
- trawlerforum.com/threads/warning-for-anyone-using-shipshape.37450/
- trawlerforum.com/threads/the-ultimate-boating-spreadsheet-for-maintenance.40660/
- cruisersforum.com/forums/f55/maintainance-tracking-software-245673.html
- cruisersforum.com/forums/f129/digital-log-books-246257.html
- yachtforums.com/threads/vessel-maintenance-app-and-software.32060/
- morganscloud.com/2018/02/23/apps-we-use-to-manage-our-boat-and-cruises/ (аналітика кругосвітників)

Legacy RSS-фід відгуків Apple (itunes.apple.com/.../rss/customerreviews/) **вимкнено** —
повертає порожній feed. Відгуки доступні лише зі сторінок apps.apple.com.

## 7. Повний перелік виявлених конкурентів

**Android (Play):** Boat Maintenance Planner (Vesselscan), Vessly, BoatMatey, Bavyn,
Boat Maintenance (IT APP Solutions), VesselMinder, VesselFile, Easy Boat Log,
Marine Logbook, Sailing Log, Nebo, Boat Fix Pro, YachtWave, SailTies, LOGBOOK (Trimutic),
Sail-Log, Mariner (Watch & Navy), SailLog, Ship's Log Book for Captains, Boat Logbook & GPS Tracker.

**iOS/web:** YachtWave, Ready4Sea, Boating Suite, ShipShape / ShipShape Pro, Vessel Vanguard,
TreMate, Boat Butler, Boat Checked, Boat Checks, Marina Maintenance Log, Wave Marine,
Boating Logbook: Skipper, Captains' Log, Boatrax, Vessel Ready, VesselTwin, VesselVault,
Boatsmart HQ, Seapilog (чартер), Deep Blue, YMP, Quartermaster, TheBoatApp, YachtPrep,
Wheelhouse, OwlMar, YachtWyse, OpenCPN logbook plugin (open source).

**Додано з форумних тредів:** Plan M8 (переможець незалежного порівняння дюжини),
Captain's Handbook, Yacht Manager ($50, Windows), Boaty, All Keeper, Idea Yacht / IDEA,
BoatOn, Vesslink, Seahub, Manage My Vessel, Latitude 365, Yachtsys, Total Superyacht,
Ship's Log, boat manager.

Разом ~60 продуктів. Категорія переповнена, але **жоден не має тракшену**.

## 8. Форумні дані

Виділено в окремий документ: **[forum-evidence.md](forum-evidence.md)** — 8 тредів,
дослівні цитати власників про гроші, звички й структуру їхніх Excel-таблиць.
Там же головний ризик проєкту (п.2) і специфікація імпортера (п.4).
