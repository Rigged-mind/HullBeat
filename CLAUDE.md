# HullBeat — правила для Claude

Офлайн-журнал ТО для яхт і катерів. Android, Kotlin, Jetpack Compose, Room.
Мови інтерфейсу: uk (основна), en, de, es.

## Збірка, перевірки, емулятор

- Gradle збирається звідси лише з двома змінними (інакше `Unable to establish
  loopback connection` через коротку назву TEMP):
  ```bash
  export JAVA_HOME=C:/Users/LaktionovVolodymyr/.jdks/jbr-21.0.11
  export JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:/Users/LaktionovVolodymyr/.gradle/udtmp
  ./gradlew :app:assembleDebug        # APK: app/build/outputs/apk/debug/
  ./gradlew :app:testDebugUnitTest
  ```
  Bash-виклики з Gradle і adb потребують `dangerouslyDisableSandbox: true`.
- `python -I tools/check_all.py` — усі генератори й перевірки в потрібному
  порядку; `--check` падає, якщо генератор переписав закомічений файл.
  Запускати перед кожним комітом.
- Емулятор і скриншоти — через навичку `/emu` (`tools/emu.sh`). Запускати
  емулятор звідси не виходить: працює лише той, який власник запустив сам.
- Зміну інтерфейсу вважати перевіреною лише після знімка на емуляторі, а не
  після зеленої збірки.
- Інструментальні тести (`app/src/androidTest`) — лише `tools/emu.sh itest`.
  Ніколи не `connectedAndroidTest`: після тестів він видаляє застосунок разом
  із даними власника на емуляторі.

## Згенеровані файли — не редагувати вручну

| Файл | Джерело | Генератор |
|---|---|---|
| `ui/theme/Color.kt`, `res/values*/colors.xml`, CSS у `prototype/index.html` | `design/palette.json` | `tools/make_palette.py` |
| `res/values*/strings_catalog.xml` | `catalog/` | `tools/make_strings.py` |
| `domain/search/SearchConfig.kt` | `design/search-ranking.json` | `tools/make_search_config.py` |

Хук блокує правку цих файлів і сам перегенеровує палітру після зміни
`palette.json`.

## Кольори — це значення, а не прикраса

- Червоний — «прострочено», бурштиновий — «скоро». Ні кнопка, ні рамка, ні
  колір секції не можуть бути червоними, бурштиновими чи помаранчевими поруч
  зі статусами.
- Зелений — бренд, ніколи не стан («зелене = все добре» не вводити).
- Колір ніколи не єдиний носій змісту: статус завжди має текст і гліф.
- Колір секції (`SectionColors.kt`) — лише для іконок, маркерів і світлого
  тла картки; не для рамок, кнопок і цілих екранів.
- Кожна нова пара «текст/фон» додається до `contrastPairs` у
  `design/palette.json`; `check_contrast.py` перевіряє лише перелічені пари.
  Поріг: 4.5 для тексту, 3.0 для UI, 7.0 у темі Sunlight.
- Material-ролі, яких немає в палітрі (`tertiaryContainer`,
  `errorContainer`, `inverseSurface` …), Compose бере зі стандартного
  бузкового Material — у Night це особливо помітно. Нову роль спершу додати
  до палітри.
- Night (червоне на чорному) захищає нічний зір біля штурвала — не робити її
  яскравішою без потреби.
- Теми: LIGHT, DARK, SUNLIGHT, NIGHT, COLORFUL (+ SYSTEM). Кольорова тема:
  темно-бірюзова шапка `#14403A`, тонований фон `#E6F0ED`, білі картки,
  плашки `#D3E5DF`. Кольори шапки — лише через `appTopBarColors()`; кожна
  `ModalBottomSheet` починається з `SheetSystemBars()`.

## Інтерфейс

- Усе, на що натискають, — не менше 56 dp (`check_touch_targets`).
- Жодного тексту інтерфейсу в Kotlin — лише ресурси (`check_hardcoded_strings`).
- Довгий текст поруч із позначкою: `Modifier.weight(1f, fill = false)`, інакше
  позначка стискається до нуля.
- Перевіряти макет в `uk`: українські рядки найдовші.
- Режим «Усі» (кілька човнів, жоден не вибраний): дія над одним човном має
  спитати, над яким, а не мовчки брати перший за абеткою.

## Переклади

- DE/ES випущені до рецензії носіями: терміни в `catalog/i18n/{de,es}.json`
  мають статус `check`, у `ok` — лише після підтвердження моряком-носієм.
- Морську термінологію перевіряє власник; позначка `ok` у `uk.json` від Claude
  означає лише впевненість Claude.

## Коміти

- Без рядка `Co-Authored-By: Claude` і без «Generated with Claude Code»
  (вимкнено в `.claude/settings.json`).
- Одна зміна — один коміт. Перед комітом прочитати `git diff --cached --stat`:
  усе, що вже в індексі, поїде разом із комітом.
- Не комітити чужі незакомічені зміни (наприклад, `ic_cat_*`) і скриншоти з
  кореня. Не пушити без прохання.
- Повідомлення англійською, у стилі історії: `fix: …`, `feat: …`, тіло
  пояснює «чому».

## Пастки середовища

- Heredoc у Bash псує `\n` і кирилицю в згенерованому коді — для таких файлів
  користуватися Write/Edit.
- Git Bash перетворює `/data/local/tmp` на Windows-шлях: `export MSYS_NO_PATHCONV=1`.
- Вивід `adb shell` закінчується на `\r`: у циклах `| tr -d '\r'`.
- Скопійована з емулятора база: брати `.db`, `-wal` і `-shm` разом.
