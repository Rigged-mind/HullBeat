# Google Play Store Listing & Release Kit for HullBeat

---

## 1. Store Listing Metadata (Метадані сторінки додатку)

### Українська (uk-UA)
- **Назва додатку (Title)** (макс. 30 символів):
  `HullBeat: Судновий журнал` *(25 символів)*  
  або:  
  `HullBeat – Судновий журнал` *(26 символів)*
- **Короткий опис (Short Description)** (макс. 80 символів):
  `Офлайн судновий журнал та регламент обслуговування вітрильних і моторних суден.` *(79 символів)*
- **Повний опис (Full Description)** (макс. 4000 символів):
```text
HullBeat — надійний офлайн судновий журнал і планувальник технічного обслуговування для власників вітрильних та моторних суден.

ЖОДНИХ ХМАР І ПОВНА ПРИВАТНІСТЬ
• Працює на 100% офлайн — у відкритому морі, на якорі чи в марині без зв'язку.
• Жодного збору даних, відстеження чи передачі інформації на зовнішні сервери.
• Додаток навіть не запитує дозволу на вихід в Інтернет. Ваші дані належать тільки вам.

ГОЛОВНІ МОЖЛИВОСТІ:

1. ЕКРАН «ЗАРАЗ» (NOW)
• Миттєвий статус судна: що потребує уваги просто зараз, що прострочено, а що підходить найближчим часом.
• Розрахунок інтервалів обслуговування за мотогодинами двигуна, календарними датами або тим, що настане раніше.
• Передрейсовий чекліст безпеки перед виходом у море.

2. КАТАЛОГ ВУЗЛІВ ТА ПАСПОРТИ ОБЛАДНАННЯ
• Повна класифікація суднових систем: силові установки, трансмісія, вітрильне озброєння, електросистема, навігація, сантехніка та корпус.
• Паспорти вузлів із характеристиками, номерами деталей, об'ємами рідин і регламентами заміни.

3. ЖУРНАЛ СЕРВІСУ ТА РОБІТ
• Повна хронологія сервісних робіт: заміна оливи, імпелера, анодів, фільтрів, обслуговування такелажу.
• Показник мотогодин записується разом із ТО — від нього рахується наступний інтервал. Фото лічильника можна прикріпити до запису.
• Фіксація витрат на деталі, паливо та сервісні послуги.

4. СКЛАД ТА ЗАПАСНІ ЧАСТИНИ
• Облік запасних частин із прив'язкою до локацій на борту (рундуки, штурманський стіл, ахтерпік).
• Контроль мінімальних залишків для автономних переходів.

5. РЕЗЕРВНЕ КОПІЮВАННЯ ТА ЕКСПОРТ
• Експорт журналу ТО та складу в Excel, суднового паспорта й сервісної книжки в PDF — для інспекторів, страхових компаній або нового власника при продажу.
• Резервна копія всієї бази з фото одним файлом у будь-яку обрану папку (Storage Access Framework).
```

---

### English (en-US)
- **App Title** (max 30 characters):
  `HullBeat: Boat Maintenance Log` *(30 characters)*  
  or:  
  `HullBeat – Boat Maintenance` *(27 characters)*
- **Short Description** (max 80 characters):
  `Offline boat maintenance logbook & schedule for sail and motor vessels.` *(72 characters)*
- **Full Description** (max 4000 characters):
```text
HullBeat is an offline-first boat maintenance logbook and servicing planner designed for sailboat and motor vessel owner-operators.

100% OFFLINE & COMPLETE PRIVACY
• Works fully offline — at sea, on anchor, or at a remote dock with zero cellular signal.
• Zero data collection, no account required, no analytics, no external servers.
• HullBeat does not even request the Android INTERNET permission. Your boat's records stay strictly on your device.

KEY FEATURES:

1. THE "NOW" DASHBOARD
• Instant vessel status: see what is due, what is overdue, and what is coming next at a single glance.
• Real marine interval logic: calculate service by engine hours, calendar date, or whichever comes first.
• Quick pre-departure safety checklist before casting off dock lines.

2. COMPONENT PASSPORTS & MARINE CATALOG
• Deep vessel hierarchy covering propulsion, drivetrain, rigging, electrical, navigation, plumbing, and hull systems.
• Technical passports storing part numbers, fluid capacities, serials, and service intervals you match to your engine's manual.

3. SERVICE LOG & ENGINE HOURS
• Complete paper trail: oil changes, raw water impellers, zinc anodes, filters, and yard haul-outs.
• Engine hours go in with each service record, and the next interval counts from them. A photo of the meter can be attached to the record.
• Track costs, parts used, and servicing notes.

4. ONBOARD SPARES & INVENTORY
• Track spares, tools, and consumables mapped to specific boat lockers and stowage locations.
• Minimum stock thresholds to prevent running out of critical parts mid-passage.

5. EXPORT & BACKUPS
• Export the service log and spares to Excel, and a buyer-ready vessel passport with its service book to PDF.
• Back up the whole log with its photos as one file in a folder of your choice (Android's Storage Access Framework).
```

---

## 2. Google Play Console Data Safety (Безпека даних)

| Питання консолі | Відповідь | Обґрунтування для перевірки Google Play |
|---|---|---|
| Does your app collect or share user data? | **No** | Застосунок є повністю офлайновим і не має дозволу `INTERNET`. Усі дані зберігаються локально в SQLite на пристрої. |
| Is all user data encrypted in transit? | **N/A** (No transfer) | Дані взагалі не передаються мережею. |
| Can users request deletion of their data? | **Yes** | Видалення даних відбувається через очищення сховища додатку або видалення додатку. |
| Does the app use advertising ID / trackers? | **No** | Жодних SDK реклами чи аналітики немає. |

---

## 3. Privacy Policy (Політика конфіденційності)

*Текст для публікації на веб-сторінці (наприклад, GitHub Pages)*:

```markdown
# Privacy Policy for HullBeat

Last updated: October 2026

HullBeat ("we", "our", or "the app") is designed with a strict privacy-first architecture. 

### 1. No Network Access
HullBeat does not include or request the Android `INTERNET` permission (`android.permission.INTERNET`). The app cannot connect to external servers, APIs, cloud services, or third-party platforms.

### 2. Information Handled Locally
All information entered into HullBeat — including vessel specifications, engine hours, maintenance records, equipment passports, photos, and inventory lists — is stored strictly and locally on your Android device in a private SQLite database.

### 3. Device Permissions Used
- **POST_NOTIFICATIONS**: Used solely on your device to trigger scheduled local maintenance reminders and due date alerts.

HullBeat does not request the camera permission. Photos are chosen through Android's system photo picker, and only the ones you pick are copied into the app.

### 4. Backups and Export
When you use the backup or export features, files (such as SQLite backups, PDF passports, or Excel logs) are saved directly to a local destination chosen by you via Android's Storage Access Framework (SAF). You maintain full control over these files. Backup files are not encrypted: if you keep one in a cloud folder, it is protected only by that service.

### 5. Third-Party Analytics and Ads
HullBeat contains zero advertisements, zero tracking libraries, and zero third-party analytics SDKs.

### 6. Contact
For any questions regarding this privacy policy:
support@hullbeat.app (or your GitHub repository issue tracker)
```

---

## 4. Signing Config Guide (Створення ключа та підпис AAB)

### Крок 1. Генерація ключа підпису (Upload Keystore)
Виконайте в терміналі (один раз):
```powershell
keytool -genkey -v -keystore release.keystore -alias hullbeat-upload -keyalg RSA -keysize 2048 -validity 10000
```
*(Збережіть `release.keystore` у надійному місці та запишіть паролі!)*

### Крок 2. Налаштування `app/build.gradle.kts`
Щоб паролі не потрапили в Git, створити файл `keystore.properties` у корені проекту (додати його в `.gitignore`):
```properties
storeFile=../release.keystore
storePassword=ВАШ_ПАРОЛЬ_СХОВИЩА
keyAlias=hullbeat-upload
keyPassword=ВАШ_ПАРОЛЬ_КЛЮЧА
```

У `app/build.gradle.kts` додати:
```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
}

android {
    ...
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```
Після цього `./gradlew bundleRelease` сформує вже підписаний `.aab` файл, готовий до завантаження в Google Play Console!
