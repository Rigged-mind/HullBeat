# Форумні дані: що власники кажуть своїми словами

Джерело: 8 сторінок, збережених вручну в `pages/` (2026-09-04), бо всі великі форуми
під Cloudflare. Текст витягнуто скриптом (depth-aware HTML-парсер).
Це найцінніша частина дослідження — тут не маркетинг вендорів, а власники, які говорять
про гроші й звички. Розділ 8 до [competitors.md](competitors.md).

Треди: The Hull Truth ×3, TrawlerForum ×2, Cruisers Forum ×1, YachtForums ×1,
morganscloud ×1 (за платною підпискою, доступний лише вступ).

## 1. Єдине системне порівняння дюжини застосунків

ScottyDee, The Hull Truth, тред «Boat Maintenance App — Experience With This?».
Купив човен, «overwhelmed with the volume of work», IT project manager за фахом.
Протестував **понад дюжину** застосунків. Його критерії — саме в цьому порядку:

1. Функціонал: групування обладнання, регламенти й нагадування, запис робіт із фото,
   історичні звіти по конкретному вузлу
2. Стабільність: «**Is it glitchy? Are there lots of little errors indicative of shoddy coding?**»
3. Юзабіліті: «**does it look like it was actually developed by a boater?**»
4. Ціна — **останній критерій**

**Переможець: Plan M8.** $40/міс, збито до $22/міс простим проханням про
«recreational boater discount». Що зачепило: інтуїтивна ієрархія, різні представлення
(календар майбутнього ТО або розріз за категорією «Running Gear»), позапланові роботи
додаються швидко й редагуються пізніше, вкладення до кожного вузла/деталі/роботи,
**живий оператор у чаті за кілька хвилин**, і доступ із iPhone/iPad/комп'ютера.

Вердикт по решті:

- **«OK, but just OK»:** TreMate, Ready4Sea, Captain's Handbook, Quartermaster
- **«Don't even bother»:** Yacht Manager, Boat Butler, Boaty, Wave, All Keeper, Boat Checks,
  Boating Suite, Idea Yacht, BoatOn, Vesslink, Vessel Vanguard
  («great platform but costs **$1,300/year** — geared towards fleet management»)

Тобто **11 із ~16 протестованих — «не витрачайте час»**. Це і є той поділ на «стерпні»
й «нікчемні», про який згадувало початкове дослідження — тепер з іменами.

Окремо, з іншого треду, про Vessel Vanguard:
> «Vessel Vanguard has only negative reviews. It appears that they are not supporting the app.»
> «i can confirm vessel vanguard is terrible!»

## 2. ⚠️ Головний ризик: попиту немає, і форум це прямо каже

Найважливіше й найнеприємніше в усьому дослідженні. У тому ж треді ScottyDee отримав
у відповідь не подяку, а спротив:

> «since you asked, **Excel is more than enough**...»

> «**Google sheets is free and more than enough.** $22/month is nothing.»

> «**Small notebook and a pen works fine for me.**»

> «Keep owners manuals and receipts in a folder and write "boat" on it.»

> «**It sounds like you're trying to project some validation for your app idea. I would not pay
> for an app to do this.** Google Sheets and an iCloud photo album work fine for me.»

> «**Maintenance on one boat doesn't rise to that level for a lot of people.** ... I don't resort
> to the latest tech just to say I did. I do it when something is either impossible to do or
> just takes too much time to do.»

Один охочий купувати проти шести, які пояснюють, що це не потрібно.

Незалежне підтвердження від інсайдера індустрії (YachtForums, olderboater, 7 127 постів):

> «**Why no great comprehensive programs? It's a very limited market.** ... The good ones require
> a tremendous amount of up front work on the part of the user. **The demand and willingness
> to pay just isn't there** it seems.»

І цитата вендора ShipShape про «net money loser» (competitors.md, п.3.6) — з третього боку
те саме.

**Три незалежні джерела кажуть, що ніша не платить.** Це не привід не робити продукт,
але це привід не планувати на нього дохід.

## 3. Що просять дослівно — і це майже завжди одне й те саме

### 3.1. Нагадування за мотогодинами — запит №1

Сформульований покупцями самостійно, без підказок:

> «an easy to use program that will **track time and let me enter hour usage** to serve as data
> which would **generate an alert to perform maintenance**. Reminders for items such as oil,
> zincs, fuel filters, strainers etc.»

> «I can keep a log on paper or excel — **it is the time/hour alert feature that seems most
> beneficial.**»

### 3.2. Спільний доступ — запит №2

> (користувач Boating Suite) «One feature I wish it had was the ability to **share the logbook
> with more than one user.** I have a captain that maintains the boat and charters it out.
> So it would be nice if I could **put a repair issue in the log and then he could see it, and
> when fixed I see it was done.** Or he could record trips and I can see the utilization.»

### 3.3. Десктоп/веб — запит №3

> «A drawback is it is phone only. **I can't add or review entries through a web interface.**»

> «**I find typing into a phone cumbersome**» (пропонують Bluetooth-клавіатуру або ввід із планшета)

> «ideally would be an App that **can be uploaded to a PC also**»

⚠️ Прямо суперечить плану «тільки Android, без бекенду». Не критично для MVP,
але експорт/імпорт мусить бути повноцінним шляхом «телефон → комп'ютер».

## 4. Чому люди в Excel — і як саме він у них влаштований

Це технічна специфікація для імпортера.
**Наївний CSV-парсер «один рядок = одна робота» провалиться.**

### Layout A — матриця (найпоширеніший у досвідчених)

> «List of tasks down the first column starting on the third row. **Years across the top row.**
> Engine hours at the start of the season across the second row. When I complete a task,
> I put the date in the corresponding cell. If I do a task more than once in a year,
> **I add another column repeating the year**... Ordinary annual tasks at the top,
> extraordinary tasks toward the bottom.»

### Layout B — плоска таблиця

> Date | Task performed | Parts used (with part number and referencing purchase invoice) |
> Engine hours | Technician (me or an actual boat mechanic)

> «**Gets a little tedious when you do multiple maintenance items in a single day**»

### Layout C — багатовкладкова книга, файл на рік

> «I use an EXCEL spreadsheet and **save a copy for each year**. I have a tab for fuel monitoring,
> a tab for winterization and end of season maintenance, a tab for spring commissioning,
> a tab with regular maintenance items/parts (description, quantity, part number), a tab with
> a to-do list, **a tab for administration reminders** (PLB & EPIRB registration, vessel
> registration, expiration dates for flares and such).»

> «I create a tab each year with my annual project plan too.»

**Отже імпортер мусить тримати:** матричний layout з роками в шапці, плоский layout,
багатовкладкові .xlsx, і один-файл-на-рік (кілька файлів → один журнал).

### Світлофор у них уже є

> «Some people don't know that Excel can do color and conditional formatting, i.e.: a category
> **turns yellow or red when a condition is met**, say number of hours pass a mark, or a date
> in time passes.»

Тобто головний екран «що горить» — не новинка для досвідчених. Референс, який вони
називають найкращим: таблиця MV Dirona. Ще одна відома — Viki Moore, astrolabesailing.com
(«The Ultimate Boating Spreadsheet»).

### Схеми в усіх різні, канонічної не буде

> «boaters have pretty varied opinions of spreadsheets like this one. **I like my own personal one,
> but I wouldn't be surprised if no one else did**... but it contains enough to let the next owner
> feel satisfied I had a clue about boating.»

→ Потрібні кастомні поля й мапінг колонок, а не жорстка схема.

### Культура обміну таблицями = канал дистрибуції

У тредах постійно: «Would anyone like to show a sample of their excel spreadsheet?»,
«Screenshots would be very helpful», «If anyone wants a copy of my workbook, send me a PM».
Але є й недовіра до файлів із невідомих джерел:
«I am concerned about opening a spreadsheet from an unknown source».

## 5. Аргументи проти таблиць — від самих же їхніх користувачів

> «**Ever try embedding images in a spreadsheet? I have, and it's horrible.**»

> «comparing **photos over time** is very helpful in troubleshooting root cause of a problem,
> or evaluating the efficacy of a new material or technique»

> «it's far from perfect, but **still easier than Excel for me. I will never get the hours of my
> life back I spent playing with Excel with little to show for it.**»

> «most apps are either **way too specific or way too broad** and inclusive that they become
> **overbearing to do simple data entry**»

> «I'm a google sheets user for the logbook. **I've tried all the apps but not found one
> that's stuck** and does everything I want it to.»

Найважливіше — збалансований відгук користувача Google Sheets:

> «I use google sheets and post links to google photos or docs to each service.
> **Not very good for predictive maintenance**, but I can very easily find when the last service
> took place. **When I sell the boat, I'll share the doc read-only with prospective buyers.**»

⚠️ Друга половина цитати підриває монетизацію через «PDF-досьє при продажу»: люди вже
роблять це безкоштовно read-only посиланням на Sheets. Перша половина, натомість, вказує
на єдину справжню перевагу застосунку — **прогнозування**.

Обхідні шляхи, які захисники Excel пропонують проти фото: зменшити роздільність і вставити,
тримати в папках і лінкувати з Excel, або перейти в OneNote.

## 6. Реальний конкурентний набір ширший за стор App Store

Люди активно використовують як «журнал обслуговування»:

- **Календар телефона.** «I simply use the calendar in my iPhone. Recurring calendar events for:
  servicing lifejackets, updating EPIRB registration, replacing flares, servicing engine,
  checking trailer wheel bearings. **It's easy to take photos of receipts and attach them
  to the event.**»
- **Google Sheets + Google Photos/Docs за посиланнями**
- **OneNote**
- **Папір.** «I've tried a few, but **always came back to paper. It's just easier.**»
  Плюс аргумент, який не обійти: паперовий навігаційний лог виживає удар молнії, коли
  вся електроніка мертва.
- **Власні AI-згенеровані інструменти** — нова загроза:
  «with proliferation of AI, I'm sure people can **make their own customized applications**
  at this point»; «I am not yet a ChatGPT user but the solution seems tailored for that generator»

Окремо про звичку, а не інструмент:
> «**I doubt that going digital will help with consistency.** I find it helps to share the duties
> such that the person coming off watch is responsible for a log entry... It keeps you accountable.»

## 7. Конкуренція з боку таких самих solo-девів

У двох тредах із восьми люди прямо анонсують свої застосунки:

> «I just finished putting together an iPhone app a couple weeks ago that handles maintenance
> logging as well as trip logs, re-usable float plans, and checklists.»

> «My partner and I are **actually coding a logbook app in our spare time** atm.»

> «I was working on a similar plan, but simply don't have the time to put into it that such
> a project deserves/requires.»

Разом із Vessly / Bavyn / VesselMinder — це вже 6+ незалежних розробників з тією ж ідеєю.
**Ідея не є дефіцитом. Дефіцит — дистрибуція й доведення до кінця.**

Ще одна деталь: у похвалі одному логбук-застосунку окремо відзначено
«**the author is very accessible**». Для solo-дева доступність автора — реальний диференціатор,
який великі не відтворять.

## 8. Суперяхтовий сегмент (YachtForums, 2019) — для повноти

Не наш ринок ($10 000+ ліцензії), але вердикти корисні:

- **IDEA** — «was the leader by far», продаж компанії 2008, розділення 2018 →
  «a lot of disconnect, lack of direction, and uncertainty»
- **Seahub** — «excellent option for equipment and maintenance management»,
  але «hasn't gotten the traction the publishers desired». Бракує логів і crew management
- **Deep Blue** — «has massive marketing $$$ and is gaining market share, but this program is
  **way too difficult to set up and it isn't intuitive**». На все відповідають
  «it can be made to do that», а з коробки не робить майже нічого
- **Manage My Vessel** — web, працює офлайн, «has the greatest potential but it's a long way
  from being there»
- **Latitude 365** — фінансовий бік. **Yachtsys** — чартери.
  **Total Superyacht** — «really just a collection of checklists»
- **Yacht Manager** — $50, Windows, «very configurable», мотогодини з лог-софту,
  інтервали «hours, time, **or both**», «lots of bells and whistles for $50»

Загальне: «most smaller size vessels aren't ready to invest $10,000+ in a maint. program».

## 9. Що з цього треба зробити продуктом

| Доказ | Рішення |
|---|---|
| «time/hour alert feature that seems most beneficial» × кілька тредів | Рушій нагадувань за мотогодинами — ядро продукту, а не модуль |
| «way too specific or way too broad... **overbearing to do simple data entry**» | Ввід у 2 дотики; усе інше опційне |
| «Is it glitchy? shoddy coding?» — критерій №2 у покупця | Стабільність важливіша за фічі. Мало екранів, усі відпрацьовані |
| «does it look like it was **developed by a boater**?» | Термінологія, категорії й регламенти мусять бути правильні. **Головний ризик для розробника, який не є власником човна** |
| «go between **a lot of tabs** to get the info» | Мінімум навігації: один екран «що горить» + пошук |
| «embedding images in a spreadsheet... **horrible**» | Фото як первинний ввід — головна перевага над Sheets |
| «**Not very good for predictive maintenance**» | Прогноз «коли настане» з ряду показників лічильника — **єдина справжня перевага над таблицею** |
| Матричні / багатовкладкові Excel-схеми, у всіх різні | Імпортер із мапінгом колонок і кастомними полями |
| «I can't add or review entries through a **web interface**» | Повноцінний двосторонній CSV/XLSX обмін як заміна вебу в MVP |
| «share the logbook with **more than one user**» | Обмін «пакетом судна» офлайн; повноцінний шеринг — після MVP |
| «read-only Sheets link to prospective buyers» вже працює безкоштовно | **Не робити PDF-досьє головним платним крючком** |
| «I would **not pay** for an app to do this» × 6 | Ціна разова й низька. Не будувати бізнес-план на цій ніші |
