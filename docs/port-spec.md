# Build prompt: native Android port of ETH Mensa

> **Historical document.** This is the specification the Android port was built from in October 2026, written for an AI coding session. It refers to the iOS sources of [ETH Mensa](https://github.com/alexandrereol/ethmensa) (`*.swift` files), which are no longer in this repository, and to the app's working names `ETH Mensa for Android` and `mrghxst/ethmensa`. The app is now called Mensa Zürich; `docs/PARITY.md` has the current state.

Written 2026-10-28 by a planning session that read every Swift file in this repository, queried the live APIs, and installed the toolchain. Where this file states a fact about the iOS code or the APIs, it was checked against the source or a live response that day. Library versions were the latest on Google Maven / Maven Central that day. Re-check them in Phase 0, because they move.

---

## 1. Goal

Build a **native Android app (Kotlin + Jetpack Compose)** in `android/` that has **feature parity with the iOS ETH Mensa app** in this repository and feels like a first-class 2026 Android app:

- Same data, same rules, same features (list, filters, smart sorting, detail with weekly menus, allergens, prices, Legi card, notifications, shortcuts, onboarding, settings, 4 languages).
- A **minimal, ultra-clean, Vercel-like look with a glass layer**: monochrome surfaces, hairline borders, Geist typography, one blue accent. Frosted and liquid glass on floating chrome. Material 3 Expressive motion and components where they fit. Fully edge-to-edge, dark mode first-class, adaptive on tablets and foldables.
- Fast and reliable: cached menus show instantly, fresh data streams in, and nothing blocks the main thread.

The user is on Android and says the existing Android mensa apps "just don't work". **Reliability of the data layer is the top priority. Polish is second. Optional extras come last.**

### Non-goals

- Don't modify the iOS/Swift sources (`ethmensa/`, `watchos/`, `widget/`, `clip/`, `ethmensa.xcodeproj/`). They are the reference implementation.
- No analytics, tracking, crash reporting SDKs or ads. The original app's promise is "no tracking or analytics whatsoever". Keep it.
- No Google Maps SDK (it needs an API key and billing). Use MapLibre with free OpenFreeMap tiles (see §9.6).
- No Play Store publishing work unless the user asks. This is a private build for the user's phone.

### License and attribution (must follow)

The iOS app is **GPLv3** (`LICENSE`). The Android port is a derivative work and is **GPLv3 too**:
- Keep `LICENSE` at the repository root.
- Add an SPDX header `// SPDX-License-Identifier: GPL-3.0-or-later` to Kotlin files.
- The About screen must say "Based on ETH Mensa for iOS by Alexandre Reol" and link to `https://github.com/alexandrereol/ethmensa`.
- Don't present the port as the official app. Don't use the original maintainer's support channels (`support@ethmensa.ch`, the WhatsApp number in `Strings.swift`) or App Store links in the Android UI. Users of an unofficial port must not be sent to the original developer for support.
- Bundled third-party assets keep their own licenses: Geist font (SIL OFL 1.1), Lucide icons (ISC), and the Apache-2.0 libraries.

---

## 2. Ground rules for this session

1. **Branch:** work on a new branch `android-port`, created from the current branch (`android-port-plan`). Commit at the end of every phase with a conventional message (`feat(android): …`). Don't push and don't merge into `main` unless the user asks.
2. **Order:** do the phases in order (§13). Each phase ends with its **acceptance checks**, which you run yourself: build, unit tests, then install on the emulator and take a screenshot. Don't start the next phase with a red build.
3. **Verify visually:** after any UI change, run the app on the emulator, take a screenshot (`android screen capture`, see §14), **look at the image**, and fix what is off before moving on. Check light and dark mode at the end of each UI phase.
4. **Stop and ask the user** only at these points:
   - Before deleting or rewriting anything outside `android/` other than `CLAUDE.md`, `.gitignore` or this file.
   - At the end: the summary (§16), plus how to install the APK on their phone.
5. **Use the project skills** in `.claude/skills/` (index in `.claude/skills/README.md`). Load the relevant skill **before** writing code in its area:
   - Build setup: `agp-9-upgrade`, `gradle-run`
   - Navigation: `navigation-3`, `navigation-event`, `adaptive`
   - Insets and system bars: `edge-to-edge`
   - Compose code quality: `compose-state-and-effects`, `compose-component-design`, `compose-animations`, `compose-performance`, `jetpack-compose-performance`
   - Kotlin: `kotlin-concurrency-and-flow`, `kotlin-api-design`, `kotlin-control-flow`
   - Tests: `testing-setup`, `compose-ui-testing-patterns`, plus the user-level `mattpocock-skills:tdd` for parser and rule tests
   - Permissions, notifications, location, deep links: `android-permissions-security`, `android-intent-security`
   - Emulator, screenshots, docs lookup: `android-cli`
   - Widget (optional phase): `glance-canonical-layouts`, `glance-appwidget-updates-and-memory`
   - Wear OS (optional phase): `wear-compose-m3`
   - Before release: `r8-analyzer`, `anr-audit-and-fix`
   - Design: the user-level skills `apple-design` (glass, spring motion, restraint) and `emil-design-eng` (polish details) apply in spirit even though they are written for the web.
6. **Look things up, don't guess:** for any API you are unsure of (Material 3 Expressive components, Navigation 3 scenes, Haze, Backdrop, MapLibre Compose), use `android docs search <query>` / `android docs fetch <url>` or read the library's README or source. These libraries changed a lot in 2025–2026.
7. **Keep it simple:** one `:app` module (plus an optional `:wear` module later). Manual dependency injection. No Hilt, no Room, no KSP.

---

## 3. Environment (already installed)

See `CLAUDE.md` for paths. Summary: JDK 21 (Temurin), Android SDK with platforms 36 and 37.0, build-tools 37.0.0, emulator with WHPX, Android Studio 2026.2.1, Android CLI 1.1.x, and AVDs `Pixel_API_37` (Pixel 9, API 37) and `medium_phone` (API 36).

**Phase 0 preflight** (run these and fix anything missing before writing code):

```
java -version                      # 21.x
android info                       # sdk: C:\Users\ghst\AppData\Local\Android\Sdk
android sdk list                   # installed packages
android emulator list              # Pixel_API_37, medium_phone
```

If `android`, `adb` or `emulator` are not found, the shell predates the PATH change. Use the full paths from `CLAUDE.md`.

---

## 4. What the iOS app does (source map)

Read the Swift file when you port its feature. The table maps each file to its Android counterpart.

| iOS source | What it does | Android target (package `io.github.mrghxst.ethmensa`) |
|---|---|---|
| `Data/API/API.swift` | Runs both providers in parallel and yields cached results first, then fresh ones, per provider | `data/MensaRepository.kt` (`Flow<ProviderResult>`) |
| `Data/API/APICache.swift` | JSON file cache per provider, valid within the same ISO week and language | `data/cache/WeeklyJsonCache.kt` |
| `Data/API/API+Perform.swift` | HTTP GET/POST + JSON decode | `data/net/Http.kt` (OkHttp + kotlinx.serialization) |
| `Data/API/ETH/ETHAPILegacy.swift` + `ETHMensaAnswerLegacy.swift` | ETH mensa directory (name, address, web, image) | `data/eth/EthDirectoryApi.kt` + DTOs |
| `Data/API/ETH/ETHAPI.swift` + `ETHMensaAnswer.swift` | ETH weekly rotas (menus, hours) merged with the directory | `data/eth/EthMenuApi.kt` + `EthMapper.kt` |
| `Data/API/ZFV/ZFVAPI.swift`, `ZFVMensaAnswer.swift`, `ZFVConverter.swift` | UZH menus via the ZFV GraphQL API | `data/zfv/ZfvApi.kt`, `ZfvDtos.kt`, `ZfvMapper.kt` |
| `Data/API/ZFV/ZFVOutletInfo.swift` + `zfv_uzh_mapping.json` | Outlet id → UZH id, name, address, lunch/dinner hours. Refreshed from GitHub every 72 h, with a bundled fallback | `data/zfv/OutletInfoRepository.kt` + `assets/zfv_uzh_mapping.json` |
| `Data/Mensa/*.swift` | Domain model: `Mensa`, `MealTime`, `Meal`, `Price`, `Campus`, opening state | `domain/model/*.kt`, `domain/OpeningState.kt`, `domain/Campus.kt` |
| `Data/Enums/*.swift` | Allergen, MealType, MeatType, filters, sort, cell type, price type | `domain/model/*.kt` |
| `Data/CoreData/ClickCountDBManager.swift` | Per-mensa open counter for smart sorting | `data/prefs/ClickCountStore.kt` (DataStore) |
| `Data/CoreData/GeoCacheDBManager.swift` + `Mensa+Location.swift` | Address → coordinates cache via the Apple geocoder | `data/geo/MensaLocator.kt` (bundled coordinates, then Android `Geocoder`, then cache) |
| `Logic/MensaDataManager.swift` | List pipeline: hide empty, search, filter, sort; tracks the loading state | `ui/list/MensaListViewModel.kt` + pure `domain/MensaListPipeline.kt` |
| `Logic/SettingsManager*.swift` | All settings and their defaults, plus migration | `data/prefs/SettingsRepository.kt` (DataStore) |
| `Logic/NavigationManager.swift` | Selected mensa, weekday override, allergen-friendly flag, deep-link resolution | `ui/AppState.kt` + Navigation 3 back stack |
| `Logic/NotificationManager.swift`, `GeofencingManager*.swift`, `Sounds/` | Weekday reminder, campus geofences, notification sounds | `notifications/*` (AlarmManager + Play services geofencing, channels per sound) |
| `Logic/NetworkManager.swift` | Offline detection | `data/net/Connectivity.kt` |
| `Views/MainView/*` | List, toolbar, filter menu, filter chips, 3 cell styles, status dot | `ui/list/*` |
| `Views/DetailView/*` | Map header, weekday picker, meal cells, share | `ui/detail/*` |
| `Views/HelperViews/LegiView.swift` | ETH Legi card WebView with injected CSS and full brightness | `ui/legi/LegiScreen.kt` + `LegiTileService.kt` |
| `Views/HelperViews/FullscreenImageView.swift` | Zoomable meal photo | `ui/image/ImageViewerScreen.kt` (telephoto) |
| `Views/SettingsView/**` | Settings, allergens, notifications, language, advanced resets, footer/about | `ui/settings/*` |
| `Views/Onboarding/*` | First-launch welcome | `ui/onboarding/OnboardingScreen.kt` |
| `Views/ModalView/*` | About / no internet / update-required modal | About screen + offline state (the update check is skipped, see §8) |
| `Views/AppViewModifiers/OpenURLViewModifier.swift` | `https://ethmensa.ch/s/{provider}/{id}` links | Deep-link intent filters + resolver |
| `App/SceneDelegate.swift`, `AppIntents/*` | Home-screen quick actions, Siri/Shortcuts, Spotlight | Static app shortcuts (`res/xml/shortcuts.xml`) |
| `widget/ShowLegiWidgetControl.swift` | Control Center button that opens the Legi card | Quick Settings tile + app shortcut |
| `App/Localizable.xcstrings` | 151 keys in en/de/fr/it | `res/values*/strings.xml`, generated by a script (§12) |
| `watchos/*` | Apple Watch app | Optional Phase 9: Wear OS |

---

## 5. Data sources (exact)

All calls run on `Dispatchers.IO` through one OkHttp client: 15 s connect and read timeouts, the `User-Agent` `ETHMensa-Android/<versionName> (+https://github.com/mrghxst/ethmensa)`, and an HTTP disk cache of 10 MB. JSON settings: `ignoreUnknownKeys = true`, `explicitNulls = false`, `coerceInputValues = true`. **Every DTO field is nullable** except where noted. The APIs omit fields freely.

**API language:** `lang = "de"` if the app's current locale language is German, otherwise `"en"`. The same rule applies to the ZFV `locale` header. For ETH, French and Italian UIs get English menu data, as on iOS. For UZH, send the app language as-is (see §5.7 item 4). The cache key includes the language.

### 5.1 ETH directory (names, addresses, images)

```
GET https://glyph.ethz.ch/eth-ws/mensas/detail?lang={en|de}
Headers: Accept: application/json
         Accept-Language: "de-DE;de;q=0.9" (de) | "en-EN,en;q=0.9" (en)
```

Response: a JSON array (22 mensas on 2026-10-28). Use these fields:

```
mensaId: Int (required)   name: String (required)   address: String?
web: String?              imageUrl: String?
```

(There are also `location`, `openingTimes`, `mealTimes`, `feedbackUrl`, `meals[]`, `defaultMensa` and `cams`. Ignore them.)

`address` contains `\n` line breaks. Keep them for display and replace them with spaces for geocoding.

### 5.2 ETH weekly rotas (menus and serving hours)

```
GET https://idapps.ethz.ch/cookpit-pub-services/v1/weeklyrotas
    ?client-id=ethz-wcms&lang={en|de}&rs-first=0&rs-size=50
    &valid-after={yyyy-MM-dd}&valid-before={yyyy-MM-dd}
Headers: as in 5.1
```

- `valid-after` = **Monday of the current ISO week** (Europe/Zurich). `valid-before` = that Monday + 14 days. (iOS uses "start of the next week + 7 days" with the device's first weekday. Use ISO Monday so it doesn't depend on locale.)
- Response root: `{"weekly-rota-array": [...]}` (26 rotas on 2026-10-28, about 540 KB). Each rota:

```
facility-id: Int  valid-from: "yyyy-MM-dd"  valid-to: "yyyy-MM-dd"?
day-of-week-array: [{ day-of-week-code: Int (1=Mon … 7=Sun, verified), day-of-week-desc, day-of-week-desc-short,
  opening-hour-array: [{ time-from, time-to,
    meal-time-array: [{ name: "Lunch"|"Dinner"|"Lunchmenu"|"Pranzo (Lunch)"|…, time-from: "HH:mm", time-to: "HH:mm",
      line-array: [{ name: "Wok Street"…,   // the line/station = meal "title"
        meal: { line-id, name, description, image-url,
          meal-price-array: [{ price: Double }],     // order: student, staff, external (1–3 entries)
          meal-class-array: [{ code, desc }],         // "Vegan","Vegetarian","Fish","Meat","Salad","Soup","Pasta"
          meat-type-array:  [{ code, desc }],         // "pork","chicken","beef","veal","turkey","duck","lamb","game"
          allergen-array:   [{ code, desc }] } }] }] }] }]
```

- Keep only rotas **valid today**: `valid-from <= today < valid-to + 1 day`. A missing `valid-to` means 2099-12-31.
- Per rota, for each day, for each opening hour, for each meal time **sorted by `time-from`**: map the lines to meals and drop lines without a `meal`. If no meals remain, skip the meal time. Otherwise create a `MealTime(weekday = day-of-week-code, start = time-from, end = time-to, type = name, meals)`.
- Join to the directory by `facility-id == mensaId`. Name, address, web and image come from the directory. **Directory mensas without a rota are still listed, with no meal times.**
- Meal mapping (from `ETHAPI.mapMeal`):
  - `title` = line `name`, falling back to the localized "Menu"
  - `name` = meal `name`
  - `description` = meal `description` with `"|\n"` replaced by `"| "`, then `"\n"` replaced by `" | "`
  - `imageUrl` = `image-url + "?client-id=ethz-wcms"`
  - `price` = `Price(student = prices[0], staff = prices[1], external = prices[2])`, each nullable
  - `mealTypes` from class descs, case-insensitive: `vegan`, `vegetarian`, `fish`. Ignore the rest.
  - `meatTypes` from meat descs: `beef`, `chicken`, `duck`, `veal`, `turkey`, `pork`. **Port improvement:** also map `lamb` and `game`. They have no iOS icon, so use a neutral meat icon.
  - `allergens` from allergen descs (see §6.4)

### 5.3 ZFV GraphQL (UZH mensas), needs an API key

```
POST https://api.zfv.ch/graphql
Headers: Content-Type: application/json
         api-key: {ZFV_API_KEY}
         locale: {en|de}
Body: {"query": "<the exact query from ethmensa/Data/API/ZFV/ZFVAPI.swift, or bruno/ZFV/Mensas Query.bru>"}
```

- **API key: use this shared public key** (the user emailed ZFV on 2026-10-28, and ZFV replied with this key, which is the general public key also used by famoser's Mensa app; it is not a personal key):
  ```
  ZFV_API_KEY=Y203MHVwaXU0OWFyeXM2MHRscWUyZncwcTpTQTJRRy83eXE5NmEzczNyRS91TjhBaysrYWl4aCs5SGhRUE9xOTk3ZzdDa1ZpdFVvQkJhK3hHN0Yyd1lLaTNu
  ```
  Verified on 2026-10-28: it returns 26 outlets with weekly menus and `weekdayNumber` values 1–7. The planning session already wrote it to `android/local.properties`. Read it from there into `BuildConfig.ZFV_API_KEY`, **and** use this value as the default in `build.gradle.kts` when the property is missing, so a fresh clone builds with UZH menus. Because the key is shared, ZFV may rotate it. If UZH comes back empty with a key set, log it and keep showing the cached menus.
- **Without a valid key the API returns HTTP 200 with `{"data":{"outlets":[]}}`** (verified). Treat "no key configured" as "provider disabled", not as an error. Don't show a UZH error to the user. If a key is set but `errors[]` is non-empty, log it and fall back to the cache.
- Response: `data.outlets[]` → `externalId`, `name`, `location.address{addressLine1, zipCode, city}`, `calendar.week.daily[] { date.weekdayNumber, menuCategories[] { category{name, slug}, menuItems[] { dish{ name, description, media[].media.url, allergens[].allergen{externalId,name}, isVegan, isVegetarian }, category{name}, prices[]{ priceCategory.externalId, amount: String } } } }`.
- `weekdayNumber`: values 1–7 (verified), assumed 1=Mon … 7=Sun like ETH. Confirm the Monday mapping against today's date in a live response and write a test for it.
- Mapping (from `ZFVConverter.swift`):
  - Outlet → Mensa: `provider = UZH`, `facilityId = info.uzhID ?? externalId.toInt()`, `name = info.name ?? outlet.name`. Location = `"addressLine1\nzip city"`, or `info.address` if empty. `imageUrl = "https://ziuzhnowweb.uzh.ch/v3/mensa/{uzhID}/image"` when there is a `uzhID`. Skip outlets with an invalid `externalId` **or with no meal times at all**.
  - Categories per day are sorted lunch → other → dinner. Service time comes from the slug: it contains `abend` or `dinner` → dinner, `mittag` or `lunch` → lunch, otherwise unknown. `type` = localized "Lunch" or "Dinner", else the category name. Start and end come from the mapping-file hours for that service time (null if unknown).
  - Meal: `title = item.category.name`, `name`/`description` with empty strings turned into null, `imageUrl = dish.media[0].media.url`. Price: externalId "1" student, "2" staff, "3" external (amount strings parsed as doubles). The price is null if all are null or 0. `mealTypes = [VEGAN]` if `isVegan`, else `[VEGETARIAN]` if `isVegetarian`. Allergens via `Allergen.fromZfv` (§6.4), de-duplicated.

### 5.4 ZFV outlet mapping file

`https://raw.githubusercontent.com/alexandrereol/ethmensa/refs/heads/main/ethmensa/Data/API/ZFV/zfv_uzh_mapping.json`. Keep pointing at the **upstream** repository so fixes from the original maintainer arrive automatically.

- Shape: `{ "<zfvOutletId>": { "uzhID": Int?, "name": String?, "address": String?, "lunch": {"start":"HH:mm","end":"HH:mm"}?, "dinner": {...}? } }`
- Copy the repository's current file into `app/src/main/assets/zfv_uzh_mapping.json` as the bundled fallback.
- Refresh it at most every 72 h (store the JSON and a timestamp in DataStore). On failure use downloaded, then bundled, then empty.

### 5.5 Not ported

`https://api.ethmensa.ch/v1/app/version` (the iOS "update required" gate compares iOS build numbers) and `dev.api.ethmensa.ch`. They are irrelevant to the Android build.

### 5.6 Caching and streaming (match `API.swift`)

- Cache file per provider in `cacheDir`: `eth_directory.json`, `eth_weekly_rotas.json`, `zfv_outlets.json`, each holding `{savedAtEpochMs, language, payload}`. A cache entry is valid if it is from the **same ISO week (Europe/Zurich)** and the same language.
- `MensaRepository.observeMensas(): Flow<ProviderResult>` starts both providers concurrently. For each provider it **first emits the cached mensas (`isCached = true`) if available, then the fresh ones (`isCached = false`)**. A failed download falls back to the cache. ETH needs the directory: if the directory fails and has no cache, ETH yields an empty list.
- The ViewModel merges per provider (ETH first, then UZH, keeping the previous provider list until the new one arrives) and tracks `allProvidersLoaded` and `allProvidersUpToDate`. Show the skeleton list only while nothing at all has arrived.
- Pull-to-refresh, app resume (`ON_START`) and a locale change trigger a reload. Cancel the previous reload when a new one starts.

### 5.7 Lessons from MensaZH (second reference app, checked 2026-10-28)

[fflopsi/mensa-zh](https://github.com/fflopsi/mensa-zh) (MIT, about 5,000 lines of Kotlin/Compose, AGP 9.4, Navigation 3 1.2.0, `adaptive-navigation3` 1.3.0, Room, Ktor) is the live Android app that now publishes as `ch.famoser.mensa` on Play and F-Droid. It reads the same two APIs. Take **ideas** from it, not code (if you ever copy code, keep its MIT notice). These items were verified against live data on 2026-10-28:

**Data fixes. Required, do them in Phase 2:**
1. **Drop placeholder meals.** On 2026-10-28, 87 of 640 ETH meals this week had `name` or `description` = `novalue`, and the iOS app would show them as real dishes. Drop a meal if its name or description, case-insensitive, contains any of: `novalue`, `closed`, `geschlossen`, `no dinner`, `kein Abendessen`, `We look forward to serving you this menu again soon!`, `Dieses Menu servieren wir Ihnen gerne bald wieder!`. Apply the same filter to ZFV dishes. A meal time with no meals left disappears; a mensa with no meal times today counts as "no menu".
2. **Normalize text.** Trim names (ETH sends values like `" Tofu Cashew Bowl"`) and collapse repeated whitespace in descriptions, after the iOS pipe rules from §5.2.
3. **Nutrition values (new, Android-only feature).** ETH meals carry `energy`, `energy-outdated`, `protein`, `fat`, `saturated-fatty-acids`, `carbohydrates`, `sugar` and `salt` as strings, per 100 g (1,072 of 1,278 meals had some on 2026-10-28). For ZFV, extend the query with `dish { stats { energy { amount } fat { amount } carbohydrates { amount } sugar { amount } fibers { amount } protein { amount } salt { amount } } }`, which is per serving. Store them nullable on `Meal` with a `per100g` or `perServing` flag. UI: a collapsed "Nutrition" row in the meal card, or in the long-press sheet. Hide it when all values are null.
4. **UZH menu language.** ZFV's `locale` header accepts `de`, `fr`, `it` and `en`, so UZH menus can follow the app language exactly. ETH stays `de`/`en` as in §5. Key the cache by the language actually requested.
5. **Errors visible but not blocking.** Like MensaZH's `NoInternet`, `ApiError` and `SlowInternet` events: if a provider fails while cached data is shown, show a snackbar ("Couldn't update UZH menus · Retry"). If a request takes longer than 8 s, show "Slow connection" with Cancel. Never replace the list with an error when cached data exists.
6. **UZH coverage.** The shared key already returns only the 26 "University Zurich" outlets. 10 of them aren't in the upstream `zfv_uzh_mapping.json` (Atrium, Uni Turm, Bar Irchel, ViCafe UZH Irchel, Café Complet, Restaurant Forum, ViCafe UZH Zentrum, Kafi Ö, Brunnenhof, UZH Retailstore). They get no hours or image, so they show UNKNOWN status. Keep iOS behavior; outlets without meals are skipped anyway. Optionally add a local `zfv_extra_hours.json` overlay later.

**UX ideas. Optional, ask the user before Phase 9 and offer them there:**
- **Favorites and hidden mensas** (long-press → Favorite / Hide; favorites pinned on top before smart sorting; manage both in Settings).
- **"Expanded" list style:** a fourth cell style that shows today's meal titles and prices inline under each mensa, so you can scan without opening details. This is MensaZH's main screen (screenshot: `metadata/en-US/images/phoneScreenshots/list-light.png` in their repo).
- **Serving hours in the row:** show today's hours (`11:00–14:00`) under the mensa name in STANDARD and LARGE cells. This one is cheap, so do it in Phase 3 if it looks clean.
- **Next week:** ETH `valid-after` = next Monday; ZFV `calendar { week(week: "next") { … } }`. Add a "Next week" toggle in the detail day picker. Design the cache key to include the week from the start, so this is easy to add.
- **Diet filter:** vegan / vegetarian chips in the filter sheet that hide non-matching meals and mensas without a matching meal.
- **"Open website"** in the detail overflow, using the ETH directory `web` field.

---

## 6. Domain model and business rules

Use immutable data classes. Write pure functions for every rule below and unit-test each one with a fixed `Clock` in the `Europe/Zurich` zone.

### 6.1 Model

```kotlin
enum class Provider(val id: String) { ETH("eth"), UZH("uzh") }
data class Mensa(val provider: Provider, val facilityId: Int, val name: String, val location: String?,
                 val webUrl: String?, val imageUrl: String?, val mealTimes: List<MealTime>) {
    val id: String get() = "${provider.id}/$facilityId"             // e.g. "eth/9", "uzh/1"
    val shareUrl: String get() = "https://ethmensa.ch/s/$id"
}
data class MealTime(val weekday: Int /*1=Mon..7=Sun*/, val start: LocalTime?, val end: LocalTime?,
                    val type: String?, val meals: List<Meal>)
data class Meal(val title: String?, val name: String?, val description: String?, val imageUrl: String?,
                val price: Price?, val mealTypes: List<MealType>, val meatTypes: List<MeatType>, val allergens: List<Allergen>)
data class Price(val student: Double?, val staff: Double?, val external: Double?)
```

- Meal rule: if `mealTypes` contains both VEGAN and VEGETARIAN, drop VEGETARIAN.
- Meal share/copy text = `[title, name, description]`, dropping nulls, joined with `\n`.

### 6.2 Weekday and "today"

- Weekday codes are **ISO: 1=Mon … 7=Sun** everywhere. `today = LocalDate.now(clock).dayOfWeek.value`.
- **iOS bug, don't copy it:** `Mensa+OpeningTimes.swift` compares against `Calendar.weekday - 1` (0=Sun), so on Sundays it never matches. Use ISO consistently.

### 6.3 Opening state (`Mensa+OpeningTimes.swift`)

`openingState(mensa, now)`: look at the meal times for today's weekday.
- If any of them lacks a start or end → `UNKNOWN`. (iOS returns UNKNOWN as soon as it reaches such an entry during iteration. Matching that exactly isn't required, but UNKNOWN whenever times are missing is the intended behavior.)
- If `now` is within `[start, end]` (inclusive) of any meal time → `OPEN`.
- Otherwise → `CLOSED` (including "no meal times today").
- UI: a green dot for OPEN, a red dot for CLOSED, nothing for UNKNOWN, and a grey dot while loading.
- The opening-hours text in the detail header = all meal times of the **selected** weekday with start and end, formatted `HH:mm–HH:mm` and joined with `, `.

### 6.4 Allergens

Enum (order matters for the settings list): `GLUTEN, CRUSTACEANS, EGGS, FISH, PEANUTS, SOYA, LACTOSE, NUTS, CELERY, MUSTARD, SESAME, SULPHUR_DIOXIDE, LUPIN, MOLLUSCS, WHEAT`, plus `Other(raw: String)`. Persist them by their raw ids: `gluten, crustaceans, eggs, fish, peanuts, soya, lactose, nuts, celery, mustard, sesame, sulphurdioxide, lupin, molluscs, wheat`.

ETH mapping (`fromETHString`, lowercased): `"gluten wheat"`→GLUTEN, `"crustaceans"|"krebstiere"`→CRUSTACEANS, `"eggs"`, `"fish"`, `"peanuts"`, `"soya"`, `"lactose"|"milk"`→LACTOSE, `"nuts"`, `"celery"`, `"mustard"`, `"sesame"`, `"sulfites"`→SULPHUR_DIOXIDE, `"lupin"`, `"molluscs"`, anything else → `Other(desc)`.
**Port fix (verified in live data):** ETH sends `"milk, lactose"` (285 times on 2026-10-28), which iOS doesn't map, so its lactose filter misses ETH meals. Map `"milk, lactose"` → LACTOSE. For German responses, also map the German descs. Fetch `lang=de` once, list the distinct `allergen-array.desc` values, and add them.

ZFV mapping (`fromZFV(id, name)`): `cerealsContainingGluten|barley|oat|rye`→[GLUTEN], `wheat|kamut|spelt`→[GLUTEN, WHEAT], `crustaceans`, `eggs`, `fish`, `peanuts`, `soybeans`→SOYA, `milk`→LACTOSE, `nuts|almond|brazilNut|cashew|hazel|macadamia|pecan|pistachio|queenslandNut|walnut`→NUTS, `celery`, `mustard`, `sesame`, `sulphites`→SULPHUR_DIOXIDE, `lupin`, `molluscs`, anything else → `Other(name ?: id)`.

- `allergenCompatibleMeals(mealTime, userAllergens)` = the meals whose allergens are disjoint from the user's.
- The "Contains: a, b, c" line shows only if the setting `showAllergens` is on and the list isn't empty.

### 6.5 Price display (`Price.getString`)

`priceType` setting, default `ALL`:
- `ALL`: `"{s}/{st}/{e} CHF"`, where a missing or zero value prints `NaN` (iOS shows `NaN`). **Port improvement:** print `–` instead of `NaN`.
- `STUDENT`: the first non-null of (student, staff, external). `STAFF`: the first non-null of (staff, external). `EXTERNAL`: external.
- Format each price with 2 decimals and `.` as the separator (`%.2f` with `Locale.ROOT`). Show it in Geist Mono.

### 6.6 Campus assignment (`Campus.swift`)

Campuses: Zentrum (47.377399461702936, 8.548341273815552), Hönggerberg (47.40862508665355, 8.50779324765029), Irchel (47.39747781967301, 8.549434449904433). A mensa's campus is the nearest one **if it is within 1000 m**, otherwise none. The campus filter matches on that.

Coordinates per mensa: **don't geocode at runtime by default.** In Phase 2, generate `assets/mensa_coordinates.json` (`{"eth/9": {"lat":…, "lng":…}, …}`) **once**: geocode each address with OpenStreetMap Nominatim (at most 1 request per second, a descriptive User-Agent, following the Nominatim usage policy). Check every point lies in Zürich, hand-fix outliers, and commit the file. At runtime: bundled coordinates first, then Android `Geocoder` (on API 33+ use the async listener API), then the cached result in DataStore keyed by address string. "Reset maps cache" clears only that DataStore cache.

### 6.7 List pipeline (`MensaDataManager.mensaList`) in this order

1. If `hideMensaWithNoMenus`, remove mensas whose meal time for the **selected list weekday** is missing or has no meals. Also remove mensas with no meal times at all.
2. Search: keep the mensas whose `name` contains the query, ignoring case and diacritics (normalize with `java.text.Normalizer` NFD and strip marks; iOS uses `localizedStandardContains`).
3. Filters:
   - **Allergen-friendly** (if active): keep the mensa unless *every* meal of *every* meal time on the list weekday contains at least one user allergen. A mensa with no meal times on that day passes, as on iOS.
   - **Open only** (if active): `openingState == OPEN`.
   - **Campus** (if not ALL): campus equals the setting.
4. Sort:
   - `SMART` (default): by click count, descending. A mensa never opened counts as −1. The sort is stable.
   - `NAME`: by name, case-insensitive (`Collator` for de-CH).
5. Return `null` (meaning "still loading", shown as a skeleton) while nothing has loaded, or while the campus filter is active and campus resolution hasn't finished. Return an empty list only when all providers are loaded.

### 6.8 Filters and their state (`MensaFilter.swift`)

Active filters, shown as removable chips in this order:
- **Open only:** `mensaShowType == OPEN`, and only while there is no weekday override.
- **Campus:** `campus != ALL`.
- **Weekday:** a weekday override is set. Choosing "today" clears the override.
- **Allergen-friendly:** the flag is on.
- **Sorting:** `sortBy != SMART`.

"Reset filters" clears all of them. "Open only" is disabled while a weekday override is set.

Persistence: `mensaShowType`, `campus` and `sortBy` are persisted settings. The weekday override and the allergen-friendly flag are session state. The allergen-friendly flag **starts on if the user has any allergens selected**, and turns on or off automatically when the allergen list goes from empty to non-empty or back.

### 6.9 Smart sorting clicks

Opening a mensa's detail increments its counter (`DataStore<Preferences>`, key `click/{mensaId}`). "Reset smart sorting" clears all counters.

### 6.10 Settings and defaults (`SettingsManager.swift`)

| Key | Type | Default | Notes |
|---|---|---|---|
| `completedOnboarding` | Boolean | false | Shows onboarding once |
| `hideMensaWithNoMenus` | Boolean | false | |
| `mensaCellType` | MINIMAL / STANDARD / LARGE | STANDARD | LARGE is for phones only (as on iOS). On tablets, fall back to STANDARD. |
| `priceType` | ALL / STUDENT / STAFF / EXTERNAL | ALL | |
| `notifications` | Boolean | false | |
| `notificationSound` | `system` / `default` / `click` | `default` | iOS ids: `default-apple`, `default`, `click` |
| `notificationTime` | LocalTime | 11:00 | |
| `locationNotifications` | Boolean | false | |
| `sortBy` | SMART / NAME | SMART | |
| `mensaShowType` | ALL / OPEN | ALL | |
| `campus` | ALL / ZENTRUM / IRCHEL / HONGG | ALL | |
| `showAllergens` | Boolean | false | |
| `allergens` | Set<String> raw ids | empty | |
| `themeMode` | SYSTEM / LIGHT / DARK | SYSTEM | Android-only addition |
| `dynamicColor` | Boolean | false | Android-only addition: wallpaper colors instead of the brand palette |

iOS-only settings that are **not** ported: iCloud key-value sync (replaced by Android Auto Backup, see §8) and the migration code.

---

## 7. Feature parity checklist

Every row must be done by the end of Phase 8 unless it is marked optional. Keep this table in `android/PARITY.md` and tick rows as you go.

| # | iOS feature | Android implementation |
|---|---|---|
| 1 | ETH and UZH mensas with weekly menus, cached then fresh | Repository flow (§5.6) |
| 2 | Three list cell styles: minimal, standard (60 pt thumbnail), large (image banner, phones only) | `MensaRow` variants + Layout option in the filter sheet |
| 3 | Open/closed status dot | `StatusDot` with a TalkBack label |
| 4 | Search by mensa name | Search field in the floating dock (§10.1) |
| 5 | Filter menu: open only, campus ×3, weekday, allergen-friendly, sort, cell size, reset | Glass bottom sheet (§10.2) |
| 6 | Active filter chips, tap to remove | Chip row under the title, animated |
| 7 | Smart sorting by open count | §6.9 |
| 8 | Pull to refresh, skeleton loading, empty state with "Reset filters and search" | M3 `PullToRefreshBox`, shimmer skeleton, empty state |
| 9 | Long-press a mensa: Open, Share | Long press → small menu or bottom sheet |
| 10 | Detail: map thumbnail, address, hours for the selected day, tap to open in maps | MapLibre thumbnail + `geo:` intent with a chooser (§10.3) |
| 11 | Detail: weekday picker (disabled while the list has a weekday override) | Connected 7-day button group |
| 12 | Detail: meal time sections, meal cells with title, diet and meat icons, price, name, description, thumbnail, allergens | §10.3 |
| 13 | Tap a meal photo → fullscreen zoom (pinch, double-tap ×3, pan) | telephoto `ZoomableAsyncImage` |
| 14 | Long-press a meal: Copy, Share | Bottom sheet with copy (clipboard) and share (Sharesheet) |
| 15 | No-menu and no-allergen-friendly-meal states | Empty states inside the detail |
| 16 | Share mensa link `https://ethmensa.ch/s/{provider}/{id}` | Sharesheet with title and icon |
| 17 | Open shared links into the right mensa, with an alert if unknown | Intent filters for `https` `ethmensa.ch` `/s/` (not verifiable, so user opt-in, see §8) plus the custom scheme `ethmensa://s/{provider}/{id}` |
| 18 | Legi card (WebView, injected CSS, max brightness, pinch disabled) | `LegiScreen` + keep screen on + brightness 1.0 |
| 19 | Legi from Control Center | **Quick Settings tile** "Legi card" + static app shortcut |
| 20 | Home-screen quick actions: currently open, Zentrum, Hönggerberg, Irchel | Static launcher shortcuts (`shortcuts.xml`) + Legi card |
| 21 | Siri, Shortcuts, Spotlight search | Covered by the app shortcuts. No equivalent needed. |
| 22 | Onboarding welcome (2 features, privacy notice) | `OnboardingScreen` (§10.6) |
| 23 | Settings: language, notifications, allergens, appearance (hide empty, price), advanced resets, about | §10.5 |
| 24 | In-app language: en, de, fr, it | Per-app language (`AppCompatDelegate.setApplicationLocales`, `locales_config.xml`) plus an in-app picker |
| 25 | Allergen settings: show in menus, my allergens | Settings > Allergens |
| 26 | Notifications: weekday reminder at a chosen time, sound choice, location-based (campus geofences) | §10.7 |
| 27 | Opening the app clears pending/delivered reminder notifications | `NotificationManagerCompat.cancelAll()` for our ids on resume (delivered only, keep the schedule) |
| 28 | Offline: "internet connection required" with retry | Offline banner over cached data, and a full-screen state with Retry when there's nothing cached |
| 29 | Settings sync over iCloud | Android Auto Backup of DataStore (`backup_rules.xml`, `data_extraction_rules.xml`), restored on a new phone |
| 30 | Tablet split view | Navigation 3 list-detail scene: two panes at ≥ 600 dp |
| 31 | Dark mode | First-class, plus a theme override setting |
| 32 | Watch app | Optional Phase 9 (Wear OS) |
| 32a | (Android extra) Nutrition values per meal | §5.7 item 3 |
| 32b | (Android extra) Placeholder `novalue` and closed meals hidden | §5.7 item 1 |
| 33 | Widget (only the Legi control on iOS) | Optional Phase 9: Glance widget "Today at {favorite mensa}" |
| — | Not ported: App Clip, SharedWithYou, Handoff, "What's new" sheet, version gate, App Store review/share, ETH Video promo, WhatsApp/email support | Explain these in `PARITY.md` |

---

## 8. Android-specific decisions (already made, don't revisit)

- **Location:** `android/` Gradle project inside this repo. `applicationId` and namespace `io.github.mrghxst.ethmensa`. App name "ETH Mensa". `versionName = "1.0.0"`, `versionCode = 1`.
- **SDK levels:** `minSdk = 29`, `targetSdk = 37`, `compileSdk = 37`. Platform `android-37.0` is installed. If AGP rejects 37, use 36 for both and note it.
- **Architecture:** single activity (`MainActivity : ComponentActivity`) → `EthMensaApp()` composable → Navigation 3 `NavDisplay`. MVVM: `ViewModel` + `StateFlow<UiState>` per screen. Pure domain functions hold the rules. A manual DI container `AppGraph` is created in `EthMensaApplication`.
- **Persistence:** DataStore Preferences for settings, clicks, geocache and outlet-info cache. JSON files in `cacheDir` for API caches. No Room.
- **Deep links:** App Links for `ethmensa.ch` **can't be verified**, because we don't control that domain's `assetlinks.json`. Declare the `https` intent filter without `autoVerify`. On Android 12+ the user must enable it once: Settings → Apps → ETH Mensa → Open by default → Add link. Add a one-time hint row in Settings > General ("Open ethmensa.ch links in the app") that opens `Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS`. Also register `ethmensa://` so links always work. Unknown ids show the "mensa not found" dialog once all providers are up to date (iOS logic in `NavigationManager.initializeLinkHandlers`).
- **Notifications:** Android channels have an immutable sound, so create **one channel per sound option** (`reminder_system`, `reminder_default`, `reminder_click`) and post to the channel matching the setting. Use `IMPORTANCE_HIGH`. Copy `default.mp3` and `click.mp3` from `ethmensa/Sounds/` to `res/raw/`.
- **Reminder scheduling:** `AlarmManager.setWindow(RTC_WAKEUP, nextWeekdayAt(time), 10 min, PendingIntent)`, with no exact-alarm permission needed. The receiver posts the notification and schedules the next weekday (Mon–Fri only, as on iOS). Re-schedule on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, and whenever the settings change. Request `POST_NOTIFICATIONS` (API 33+) when the user turns notifications on. On denial, revert the toggle and show a dialog with "Open settings".
- **Location-based notifications** (iOS semantics, with the iOS bug fixed): when on, register Play services geofences around the 3 campuses (radius 500 m, ENTER and EXIT). **On ENTER:** if notifications are on, (re)arm the reminder. **On EXIT:** cancel pending reminders. While location-based is on, a reminder is posted **only if the user is inside a campus geofence** (keep a flag in DataStore). Permission flow: fine location first, then background location as a separate step with an explanation (Android 11+ sends the user to Settings). If either is denied, revert the toggle. Re-register the geofences after boot. **iOS bug, don't copy:** `GeofencingManager.makeGeoRegion` passes `(long, lat)` into `CLLocationCoordinate2DMake(lat, long)`, so its iOS geofences are in the wrong place.
- **Legi card:** `WebView` with JavaScript and DOM storage enabled and persistent cookies (`CookieManager.setAcceptCookie(true)`, `flush()` on pause). Inject the same CSS as `LegiView.swift` in `onPageFinished`, using our theme's surface and text colors. Disable pinch zoom (`setSupportZoom(false)`, `builtInZoomControls = false`). While the screen is visible: `window.attributes.screenBrightness = 1f` and `FLAG_KEEP_SCREEN_ON`, both restored in `onPause`/`dispose`. Open external links in a Custom Tab. Back navigates inside the WebView first. Handle the ETH SSO login inside the WebView.
- **Quick Settings tile:** a `TileService` "Legi card". On click, `startActivityAndCollapse(PendingIntent)` (API 34+ signature, with a fallback) to `MainActivity` with the deep link `ethmensa://legi`.
- **Backup:** `android:allowBackup="true"` with rules that include the DataStore files and exclude the caches.
- **Privacy:** no network calls besides the ones in §5, the MapLibre style and tile servers (§9.6), and the image hosts. Add the Lucide, Geist and OpenFreeMap/OpenStreetMap attributions in About > Licenses.
- **Network security:** HTTPS only (`usesCleartextTraffic=false`).

---

## 9. Design system: "Vercel minimal × glass × Material 3 Expressive"

The look: **monochrome, quiet, precise.** Hairline borders instead of heavy shadows, generous whitespace, one accent color, tabular mono numbers. Glass appears **only on floating chrome** (top bar when scrolled, bottom dock, sheets, overlay buttons), never on content cards. Motion is springy and interruptible (M3 Expressive motion scheme). Read the `apple-design` skill once for the principles of glass, depth, restraint and spring motion, and apply them in Compose.

### 9.1 Color tokens (define in `ui/theme/Color.kt`, map them into a custom `MaterialTheme.colorScheme` + `LocalEthColors`)

| Token | Light | Dark |
|---|---|---|
| background | `#FAFAFA` | `#0A0A0A` |
| surface (cards) | `#FFFFFF` | `#111111` |
| surfaceMuted | `#F4F4F5` | `#1A1A1A` |
| border (hairline, 1 dp) | `#EAEAEA` | `#262626` |
| textPrimary | `#0A0A0A` | `#EDEDED` |
| textSecondary | `#666666` | `#A1A1A1` |
| textTertiary | `#8F8F8F` | `#737373` |
| accent (ETH blue, from iOS `AccentColor`) | `#205CAE` | `#4C8DF6` |
| onAccent | `#FFFFFF` | `#FFFFFF` |
| accentSoft (selected chip bg) | `#E8F0FB` | `#14243D` |
| open | `#16A34A` | `#22C55E` |
| closed | `#DC2626` | `#EF4444` |
| vegan/vegetarian icon | `#16A34A` | `#22C55E` |
| fish icon | `#2563EB` | `#60A5FA` |
| glassTint | `#FFFFFF` at 70 % | `#141414` at 62 % |
| glassStroke | `#000000` at 6 % | `#FFFFFF` at 10 % |

Meat icon tints (from `MeatType.swift`): beef brown `#8B5E3C`, chicken amber `#EAB308`, duck orange `#F97316`, veal gray `#9CA3AF`, turkey brown `#A16207`, pork pink `#EC4899`, lamb and game `#78716C`.

`dynamicColor = true` swaps in `dynamicLight/DarkColorScheme` but keeps the open/closed and diet colors.

### 9.2 Typography: Geist (OFL), bundled

Download the release zip `geist-font-v1.8.2.zip` from `github.com/vercel/geist-font` (latest release on 2026-10-28; use a newer one if there is one). Copy the static TTFs into `res/font/`: `geist_regular`, `geist_medium`, `geist_semibold`, `geist_bold`, `geist_extrabold`, `geist_mono_regular`, `geist_mono_medium`. Add `OFL.txt` to `app/src/main/assets/licenses/`.

| Role | Font | Size / line height | Weight | Letter spacing |
|---|---|---|---|---|
| Onboarding display | Geist | 44 / 48 sp | ExtraBold | −0.03 em |
| Large title (collapsing) | Geist | 32 / 38 sp | SemiBold | −0.02 em |
| Screen title (collapsed) | Geist | 18 / 24 sp | SemiBold | −0.01 em |
| Section label ("LUNCH") | Geist | 12 / 16 sp, uppercase | Medium | +0.06 em, textSecondary |
| Row title (mensa name) | Geist | 17 / 22 sp | Medium | −0.01 em |
| Meal title (line) | Geist | 16 / 22 sp | SemiBold | 0 |
| Meal name | Geist | 15 / 20 sp | SemiBold | 0 |
| Body / description | Geist | 15 / 21 sp | Regular | 0, textSecondary |
| Caption / allergens | Geist | 13 / 18 sp | Regular, italic | textTertiary |
| Price, times | Geist Mono | 13 / 18 sp | Medium | tabular |

### 9.3 Shape, spacing and elevation

- Corner radii: cards 20 dp, thumbnails 14 dp, map thumbnail 16 dp, chips and buttons fully rounded, bottom sheets 28 dp top, floating dock 32 dp, app icon squircle in onboarding.
- Spacing on a 4 dp grid. Screen gutter 16 dp (24 dp at ≥ 600 dp width), card padding 16 dp, gap between list cards 12 dp (iOS uses 15 pt).
- Elevation: none on content. Cards use a 1 dp `border` and no shadow. Floating glass gets one soft shadow: `0 8 24 rgba(0,0,0,0.08)` in light, none in dark, plus `glassStroke`.

### 9.4 Glass layers

- Dependencies: **Haze** `dev.chrisbanes.haze:haze` + `haze-materials` 2.0.1 for frosted blur, and **Backdrop** `io.github.kyant0:backdrop` 2.0.1 (Kyant's "AndroidLiquidGlass") for refractive liquid glass. Read both READMEs before use. Their APIs changed in 2.x.
- **Frosted (Haze):** the list and detail content is the `hazeSource`. The top bar (once the content scrolls under it), the filter chip row background and bottom sheets use `hazeEffect` with a style close to `HazeMaterials.ultraThin` tinted with `glassTint`.
- **Liquid glass (Backdrop):** only on the floating bottom dock and its buttons, and on the circular overlay buttons in the detail hero (back, share). Use a subtle lens/refraction, a highlight, and a 1 dp `glassStroke`. Keep it subtle (Vercel restraint): no rainbow edges, no heavy distortion.
- **Fallbacks:** API 33+ gets the full effects. API 31–32 gets Haze blur only (no lens). API 29–30 gets an opaque `glassTint` at 92 % with no blur. Put this behind one `Modifier.glass(style)` in `ui/components/Glass.kt` so screens never branch on API level.
- Contrast: text on glass must meet WCAG AA. Add a tint scrim if needed and check it in the screenshots over busy food photos.

### 9.5 Motion (M3 Expressive)

- `MaterialTheme(motionScheme = MotionScheme.expressive())` from material3 1.5.0-beta02 (§11). Opt in to `@ExperimentalMaterial3ExpressiveApi` where needed.
- **Shared element** from the list thumbnail (standard/large cells) to the detail hero image (`SharedTransitionLayout` + `sharedElement`, wired through Navigation 3).
- **Predictive back** on detail, settings and sheets (`navigation-event` skill).
- Chips animate in and out (`animateItem`, fade + scale 0.9 → 1). List reorders animate with `animateItem()`.
- Status dot: a soft 2 s pulse when OPEN. Respect "Remove animations" (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → no pulse).
- Haptics: `HapticFeedbackType.SegmentTick` on segmented changes, `ToggleOn`/`ToggleOff` on switches, `LongPress` on long-press menus.
- Durations: no tweens over 350 ms. Prefer springs (`spring(dampingRatio = 0.8f, stiffness = 380f)` for UI, the expressive scheme for components).

### 9.6 Map thumbnail

- `org.maplibre.compose:maplibre-compose` 0.20.0 (check the README for the Android setup and `MaplibreMap` API) with the style `https://tiles.openfreemap.org/styles/positron`: grayscale, free, no key (verified reachable). In dark mode, use the `dark` style variant if OpenFreeMap provides one (check), otherwise positron with a darkening overlay.
- Non-interactive (all gestures off), zoom 15.5, centered on the mensa, with a small accent dot marker. Size 96 × 96 dp, radius 16 dp, and the attribution "© OpenFreeMap © OpenStreetMap" reachable through an info button or About > Licenses, as the tile license requires.
- **Fallback:** if MapLibre causes trouble (binary size, crashes in lists, blank tiles), render a static monochrome placeholder with a pin icon. **Don't block the release on the map.** Tap opens `geo:{lat},{lng}?q={lat},{lng}({Uri.encode(name)})` via `Intent.createChooser`.

### 9.7 Icons

- **Lucide** (`com.composables:icons-lucide-android` 2.2.1; check the artifact name on Maven Central) for UI icons: `Search`, `SlidersHorizontal` (filter), `IdCard` (Legi), `Settings`, `Share2`, `MapPin`, `Clock`, `Calendar`, `ShieldCheck` (allergen-friendly), `ArrowUpDown` (sort), `LayoutList` (cell size), `X`, `ChevronRight`, `Leaf` (vegetarian), `Sprout` or a filled leaf (vegan), `Fish`, `Bell`, `Globe`, `CircleAlert` (allergens), `Copy`, `Utensils`, `CookingPot` (empty states), `WifiOff`.
- **Meat icons:** copy the PNGs from `ethmensa/App/Assets.xcassets/{beef,chicken,duck,pork,turkey}.imageset/` (48 px) into `res/drawable-nodpi/`, or better, trace them as vector drawables for crispness and tinting. Veal uses beef in gray, as on iOS. Lamb and game use `Beef` from Lucide in gray.
- **App icon:** an adaptive icon from `ethmensa/App/AppIcon.icon/`. Background = linear gradient `#215CAF` → `#08407E` (from `icon.json`). Foreground = `Assets/front.png` inside `AppIcon.icon` (scale it into the 66 dp safe zone). Monochrome (themed icons) = the foreground silhouette, or `ethmensa_tinted.png` from `AppIcon.appiconset`. Generate the densities with a small Python script (Pillow) under `android/tools/`. Also use the icon as the Splash Screen API icon on a background-color splash.

---

## 10. Screens

Layouts are for phone portrait. At ≥ 600 dp, list and detail sit side by side through Navigation 3's list-detail scene (`adaptive` skill). Everything is edge-to-edge and respects insets (`edge-to-edge` skill).

### 10.1 Mensa list (home)

```
┌────────────────────────────────────────┐
│  (status bar, content scrolls under)    │
│  [IdCard ◯]                 [Settings ◯]│  ← circular glass icon buttons
│  ETH Mensa                              │  ← large title, collapses into a frosted top bar
│  (Open only ✕) (Zentrum ✕) (Name ✕)     │  ← active filter chips, only if any
│ ┌────────────────────────────────────┐ │
│ │ [img 60] Mensa Polyterrasse    ● › │ │  ← STANDARD cell: bordered card
│ └────────────────────────────────────┘ │
│ ┌────────────────────────────────────┐ │
│ │ [img]    Clausiusbar           ● › │ │
│ └────────────────────────────────────┘ │
│              …                          │
│   ╭──────────────────────────────╮      │
│   │ 🔍 Search mensas      │ [⧉] │       │  ← floating liquid-glass dock (M3 Expressive
│   ╰──────────────────────────────╯      │     HorizontalFloatingToolbar look), above nav bar
└────────────────────────────────────────┘
```

- Title: `LargeFlexibleTopAppBar` (expressive) or a custom collapsing header. The title is the app name.
- **Cells:**
  - MINIMAL: name + dot + chevron, 56 dp tall.
  - STANDARD: 60 dp rounded thumbnail (app icon as fallback) + name + dot + chevron.
  - LARGE (phone only): a 140 dp image banner with a bottom bar holding the name, dot and chevron, falling back to MINIMAL when there's no image.
  - All cells: ripple, `animateItem()`, long-press menu (Open, Share), TalkBack label "{name}, open now" or "{name}, closed".
- **Dock:** tapping search expands it into an M3 `SearchBar` (expanded state) with results filtering live, or simply focuses an inline text field. Choose whichever looks cleaner. The filter button shows a small accent badge when any filter is active and opens the filter sheet.
- **Loading:** 10 skeleton cards of the current cell style with a shimmer (respect reduced motion).
- **Empty:** a centered CookingPot icon, "No results", "No results for the selected filters." and a primary pill "Reset filters and search".
- **Offline** with cached data: a slim glass banner under the title ("Offline · showing saved menus"). Offline with no data: a full-screen WifiOff state, "Internet connection required" and Retry.
- **Pull to refresh** with the expressive loading indicator.

### 10.2 Filter sheet (`ModalBottomSheet`, glass background)

Sections, each a grouped bordered card:
1. **Status:** switch "Open only" (disabled with the hint "Not available when a weekday is selected" while an override is set).
2. **Location:** three toggle chips (Zentrum, Hönggerberg, Irchel). Single choice, tap again to clear.
3. **Day:** a connected button group Mon–Sun with "Today" marked by a dot. Selecting today clears the override.
4. **Allergen-friendly:** switch with the subtitle "At least one meal without your allergens". Shown only if the user has allergens.
5. **Sort:** segmented Smart / Name.
6. **Layout:** segmented Minimal / Standard / Large (Large only on phones).
7. **Reset filters:** a text button, shown only if any filter is active.

Changes apply immediately (the list animates behind the sheet).

### 10.3 Mensa detail

```
┌────────────────────────────────────────┐
│ [hero image 240dp, fades into bg]       │  ← shared element from the list thumbnail
│ (◀)                               (⇪)   │  ← liquid-glass circular buttons
│ Clausiusbar                             │  ← title over the gradient, collapses into the top bar
│ ┌────────────────────────────────────┐ │
│ │[map 96] CLA D                       │ │  ← info card: address (3 lines max)
│ │         Tannenstrasse 3, 8006 Zürich│ │
│ │         🕐 11:00–14:00              │ │  ← hours for the selected weekday (mono)
│ └────────────────────────────────────┘ │
│ [Mo][Tu][We][Th][Fr][Sa][Su]            │  ← connected button group, today dotted
│ 🛡 Allergen-friendly (if active)        │
│ LUNCH                                   │  ← section label
│ ┌────────────────────────────────────┐ │
│ │ Wok Street 🐔        13.20/14.20/16.50 CHF│ ← title + icons + price (mono, right)
│ │ KWANG PHANAENG                [img] │ │  ← name (semibold) + 64dp thumbnail
│ │ Chicken | Panang curry sauce …      │ │  ← description (secondary)
│ │ Contains: Gluten, Celery            │ │  ← if showAllergens
│ └────────────────────────────────────┘ │
│  … more meals (one card per meal or one card per section with dividers; choose the cleaner one) │
│ DINNER …                                │
└────────────────────────────────────────┘
```

- The weekday group is disabled while the list has a weekday override (iOS behavior). Opening a detail resets the selected weekday to today unless there is an override. The click count increments when the detail is opened (§6.9).
- When a meal title is all uppercase, show it capitalized (iOS: `.capitalized`). Keep the meal *name* as delivered.
- Without a hero image: no hero, just a normal top bar with the title.
- Empty states: "No menu" (CookingPot, "There is no menu on this day.") and "No allergen-friendly meal".
- The share button shares `shareUrl` with the mensa name as the title.
- Pull to refresh reloads everything.

### 10.4 Image viewer

A full-screen black dialog or destination, `ZoomableAsyncImage` (telephoto) with pinch, double-tap zoom and pan, a close button (glass, top start), and swipe down to dismiss. It loads from Coil's cache.

### 10.5 Settings (full-screen destination; grouped inset cards like Android 16 settings)

- **General:** Language (opens a picker with en, de, fr, it and the note "applies immediately", using per-app locales), Notifications →, Allergens →, and "Open ethmensa.ch links in the app" (§8).
- **Appearance:** Hide mensas without menus (switch), Price to show (All / Student / Staff / External, with the footer "Choose which price to show"), Theme (System / Light / Dark), Wallpaper colors (switch, Android 12+ only).
- **Backup:** text explaining that settings are backed up with the Android device backup.
- **Advanced** (red footer text "These actions can't be undone."): Reset smart sorting, Reset maps cache, Reset settings, Reset settings and data. Each one asks for confirmation with a destructive button.
- **About:** app icon, "ETH Mensa for Android", version and build, "Based on ETH Mensa for iOS by Alexandre Reol" (link), "Source code" (link to `github.com/mrghxst/ethmensa`; it's private, so link it but say "private repository"), License: GPLv3 (shows `LICENSE`), Open-source licenses (Geist OFL, Lucide ISC, Apache libraries, OpenStreetMap/OpenFreeMap), the privacy statement "No tracking or analytics." (text from `ONBOARDING_PRIVACY_POLICY_MESSAGE`, adapted).
- **Allergens screen:** switch "Show in menus" + "My allergens" with 15 check rows (localized names, capitalized). The allergen-friendly filter flag follows the rule in §6.8.
- **Notifications screen:** Enable notifications (switch, footer), then if on: Sound (System default / Default / Click, previewing each sound when tapped), Time (M3 `TimePicker` dialog, footer), and Location-based (switch, footer). Behavior as in §8.

### 10.6 Onboarding (first launch only)

A full screen: app icon (78 dp squircle), "Welcome to" + newline + "ETH Mensa" in the accent color (display style), and two feature rows with 44 dp icons: Utensils, "Check weekly menus" / description, and IdCard, "Legi Card in-app" / description, **rewritten for Android** (mention the Quick Settings tile and app shortcut instead of Control Center or the Lockscreen). Bottom bar: the privacy notice + "Privacy policy" link (to the About privacy text) and a full-width pill "Continue". On tablets, two panes (icon and title left, features right), as iOS does with `ArrangementView`. Set `completedOnboarding = true` when done.

### 10.7 Notifications content

Body: "Check out today's Mensa menu!" (key `CHECK_OUT_TODAYS_MENSA_MENU!`). Tapping it opens the app on the list. Small icon: a monochrome vector of the app glyph. The notification's "Settings" action and channel settings deep link to the in-app Notifications screen (iOS `openSettingsFor`).

### 10.8 App shortcuts (`res/xml/shortcuts.xml`)

`currently_open` (sets show-type OPEN and clears other filters, like `CurrentlyOpenIntent`), `zentrum`, `hongg`, `irchel` (reset filters, set campus), and `legi` (opens the Legi card). Each uses an intent with `ethmensa://action/{id}` handled in `MainActivity`. Icons are monochrome Lucide-style vectors on an accent background. Labels come from the existing localized keys (`CURRENTLY_OPEN`, `ZENTRUM`, `HONGGERBERG`, `IRCHEL`, `LEGI_CARD`).

---

## 11. Tech stack and versions (latest on 2026-10-28; check for newer stable versions in Phase 0)

Use a version catalog (`gradle/libs.versions.toml`) and Kotlin DSL.

| Component | Version | Notes |
|---|---|---|
| Gradle wrapper | 9.8.2 | Use what AGP 9.5 requires or recommends |
| Android Gradle Plugin | 9.5.0 | AGP 9 has **built-in Kotlin**: don't apply `org.jetbrains.kotlin.android` (see the `agp-9-upgrade` skill) |
| Kotlin | 2.4.21 | Plugins `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.kotlin.plugin.serialization` at the same version |
| JDK / toolchain | 21 | `jvmToolchain(21)` or AGP's `compileOptions` |
| Compose BOM | 2026.09.00 | |
| material3 | **1.5.0-beta02** (pinned over the BOM) | Needed for the Expressive components and motion scheme. Fall back to the BOM's 1.4.x if something is broken, and replace the expressive-only components. |
| material3-adaptive + adaptive-navigation3 | latest stable (adaptive 1.3.0) | List-detail scene for Navigation 3. Confirm the artifact name. |
| Navigation 3 | `androidx.navigation3:navigation3-runtime` / `-ui` 1.2.0 | + `androidx.lifecycle:lifecycle-viewmodel-navigation3` (stable matching lifecycle 2.11.0) |
| Lifecycle | 2.11.0 | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| Activity Compose | 1.13.0 | |
| Core splashscreen | 1.2.0 | |
| AppCompat | latest stable | Only for `AppCompatDelegate.setApplicationLocales` (or use `LocaleManager` on 33+ and AppCompat below) |
| DataStore Preferences | 1.3.0 | |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-serialization-json | 1.12.0 | |
| OkHttp | 5.5.1 | + `mockwebserver3` 5.5.1 for tests |
| Coil 3 | 3.6.3 | `coil-compose` + `coil-network-okhttp` |
| telephoto | 0.19.0 | `me.saket.telephoto:zoomable-image-coil3` |
| Haze | 2.0.1 | `haze`, `haze-materials` |
| Backdrop (liquid glass) | 2.0.1 | `io.github.kyant0:backdrop` |
| MapLibre Compose | 0.20.0 | `org.maplibre.compose:maplibre-compose` |
| Play services location | 21.4.0 | Geofencing only |
| WorkManager | 2.12.0 | Only if needed (for example the 72 h mapping refresh). Alarms handle reminders. |
| androidx.browser | 1.10.0 | Custom Tabs |
| androidx.webkit | 1.18.0 | WebView helpers (`WebViewFeature`, dark mode) |
| Glance appwidget | 1.2.0 | Optional Phase 9 |
| Lucide icons | 2.2.1 | Confirm the artifact. If unusable, use Material Symbols Rounded vector drawables. |
| Tests | JUnit 4 or 5, Turbine 1.2.1, Robolectric 4.17, Compose UI test, optionally Roborazzi | |
| Static checks | Android Lint (warnings as errors for `MissingTranslation`, `UnusedResources` at the end); Spotless + ktlint optional | |

Release build: R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`), keep rules for kotlinx.serialization DTOs (or `@Keep`), a debug-signed APK for sideloading (`assembleRelease` with a local keystore that is **git-ignored**, or simply install `assembleDebug`).

---

## 12. Project layout

```
android/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew(.bat)
  local.properties            # git-ignored: sdk.dir=C\:\\Users\\ghst\\AppData\\Local\\Android\\Sdk, ZFV_API_KEY=...
  PARITY.md                   # §7 checklist, ticked as you go
  tools/
    xcstrings_to_android.py   # Localizable.xcstrings → res/values*/strings.xml
    make_icons.py             # adaptive icon layers + densities from AppIcon.icon
    geocode_mensas.py         # one-off: generates assets/mensa_coordinates.json (Nominatim, 1 req/s)
    capture_fixtures.sh/.ps1  # one-off: records live API responses into app/src/test/resources/fixtures
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/assets/          # zfv_uzh_mapping.json, mensa_coordinates.json, licenses/
    src/main/res/             # values, values-de, values-fr, values-it, font, raw, drawable, xml, mipmap-*
    src/main/kotlin/io/github/mrghxst/ethmensa/
      EthMensaApplication.kt, MainActivity.kt, AppGraph.kt
      data/net/ (Http.kt, Connectivity.kt)
      data/cache/ (WeeklyJsonCache.kt)
      data/eth/ (EthDirectoryApi.kt, EthMenuApi.kt, EthDtos.kt, EthMapper.kt)
      data/zfv/ (ZfvApi.kt, ZfvDtos.kt, ZfvMapper.kt, OutletInfoRepository.kt)
      data/geo/ (MensaLocator.kt)
      data/prefs/ (SettingsRepository.kt, ClickCountStore.kt)
      data/MensaRepository.kt
      domain/model/ (Mensa.kt, MealTime.kt, Meal.kt, Price.kt, Allergen.kt, MealType.kt, MeatType.kt, Campus.kt, enums)
      domain/ (OpeningState.kt, MensaListPipeline.kt, Filters.kt, PriceFormatter.kt, Weekdays.kt)
      notifications/ (Channels.kt, ReminderScheduler.kt, ReminderReceiver.kt, BootReceiver.kt, GeofenceManager.kt, GeofenceReceiver.kt)
      legi/ (LegiTileService.kt)
      ui/ (EthMensaApp.kt, Navigation.kt, AppState.kt)
      ui/theme/ (Color.kt, Type.kt, Shape.kt, Theme.kt, Motion.kt)
      ui/components/ (Glass.kt, StatusDot.kt, MensaRow.kt, MealCard.kt, FilterChipRow.kt, FloatingDock.kt, SectionLabel.kt, GroupedCard.kt, Skeleton.kt, EmptyState.kt, MapThumbnail.kt, DietIcons.kt)
      ui/list/, ui/detail/, ui/filter/, ui/settings/, ui/legi/, ui/image/, ui/onboarding/
    src/test/kotlin/…         # JVM unit tests (parsers, mappers, rules, pipeline, repository with MockWebServer)
    src/test/resources/fixtures/
    src/androidTest/kotlin/…  # Compose UI tests
```

### Localization conversion (`tools/xcstrings_to_android.py`)

- Input: `ethmensa/App/Localizable.xcstrings` (151 keys; en, de, fr, it, all complete). The output is regenerated, not hand-edited. Android-only strings go in a separate `strings_android.xml` per locale, written by hand in all 4 languages.
- Key names: lowercase, with every non `[a-z0-9]` character turned into `_`, collapsed, and stripped at the edges. Prefix with `n_` if the key starts with a digit. Keep a `xcstrings_key → android_name` map in the script output for reference.
- Placeholders: `%@` → `%1$s`, `%lld` → `%1$d` (positional when there are several). Escape `'`, `"`, `@` and `?` at the start, `&`, `<`, and `\n`.
- Plurals: `%lld_ALLERGENS_SELECTED` has plural variations. Generate `<plurals>`.
- Keys used only by iOS-only features can be skipped, but generating everything is simpler. Mark unused ones with `tools:ignore="UnusedResources"`.
- iOS wording that is wrong on Android must be overridden in `strings_android.xml`: anything that mentions iCloud, App Store, Control Center, Lockscreen, Siri or Apple. Write proper de/fr/it translations; the iOS translations show the tone.
- Add `res/xml/locales_config.xml` (en, de, fr, it) and `android:localeConfig`.

---

## 13. Phases (do them in order; every phase ends green)

### Phase 0: Preflight and decisions (≈ 15 min)
1. Run the §3 checks. Create branch `android-port`.
2. Re-check the §11 versions on Google Maven / Maven Central (and AGP ↔ Gradle ↔ Kotlin compatibility). Update the table in this file if they changed.
3. Check that `android/local.properties` contains `ZFV_API_KEY` (§5.3) and `sdk.dir`. The app must still behave correctly if a key ever returns no outlets.
4. Boot `Pixel_API_37` (`android emulator start Pixel_API_37`, run in the background; add `--headless` if no window is needed) and wait until `adb shell getprop sys.boot_completed` prints `1`. The planning session checked this on 2026-10-28: it booted with WHPX, and `android screen capture --output=<file>.png` worked. Leave it running for later phases.

**Done when:** tools respond, the emulator boots, and the versions are confirmed.

### Phase 1: Project skeleton
1. Create the Gradle project in `android/` by hand or with `android create` (then clean it up to match §12): version catalog, AGP 9.5, Kotlin 2.4, Compose, minSdk 29, target and compile 37, `buildConfig = true`, `BuildConfig.ZFV_API_KEY` from `local.properties` (empty string if missing).
2. `MainActivity` with `enableEdgeToEdge()`, the splash screen and an empty themed `EthMensaApp`.
3. Theme: §9.1 colors, §9.2 Geist fonts, §9.3 shapes, expressive motion, dark/light/dynamic switching, `LocalEthColors`.
4. App icon (adaptive + monochrome) via `tools/make_icons.py`.
5. Localization script and generated `strings.xml` (all 4 locales), `locales_config.xml`.
6. `.gitignore` already covers the `android/` build outputs and `local.properties`. Check it.

**Done when:** `./gradlew :app:assembleDebug` passes, the app installs and launches on the emulator showing the themed empty screen and the right icon, and a screenshot has been reviewed.

### Phase 2: Data layer (headless, test-first)
1. Record fixtures with `tools/capture_fixtures`: ETH directory (en, de), weekly rotas (en, de), the ZFV response (with the key from §5.3), and the mapping JSON.
2. DTOs + mappers + `WeeklyJsonCache` + `OutletInfoRepository` + `MensaRepository` flow (§5).
3. The §5.7 data fixes (placeholder filter, text normalization, nutrition fields, UZH locale), then the domain rules (§6): opening state, allergens (including the `milk, lactose` fix and the German descs), price formatting, campus, the list pipeline, filters, smart sort.
4. `tools/geocode_mensas.py` → `assets/mensa_coordinates.json` for every ETH mensa and UZH outlet known from the mapping file. Check every point is within Zürich and commit the file.
5. `SettingsRepository` (DataStore) with every §6.10 key and default, and `ClickCountStore`.
6. Unit tests: parsing each fixture; placeholder meals (`novalue` and closed notices) dropped; mapping rules (description pipes, image URL suffix, price ordering with 1, 2 and 3 entries, vegan/vegetarian de-dup, allergen maps, rotas valid today, directory mensas without a rota still listed); `openingState` across the boundaries (before, at start, inside, at end, after, missing times, Sunday); the pipeline (each filter, combinations, smart vs name sort, hide-empty, search with umlauts such as "Hönggerberg" matching "honggerberg"); cache week and language validity; the repository emitting cached then fresh, and falling back on a 500 or timeout (MockWebServer).

**Done when:** `./gradlew :app:testDebugUnitTest` passes with meaningful coverage of `domain/` and `data/`, and a debug screen or log shows the real merged mensa list from the live APIs.

### Phase 3: Mensa list
List screen (§10.1) with all three cell styles, the status dot, skeleton, empty and offline states, pull to refresh, search, the filter chip row, the floating glass dock and the filter sheet (§10.2). Long-press menu and share. Glass system `Modifier.glass` (§9.4) with its fallbacks.
**Done when:** screenshots of light and dark, each cell style, the filter sheet open, an active-chips state, the empty state, and a check at API 36 (`medium_phone`) all look right; filtering and sorting behave per §6.7–6.8 (spot-check against the unit-tested pipeline).

### Phase 4: Detail and navigation
Navigation 3 back stack (`List`, `Detail(mensaId)`, `Settings…`, `Legi`, `ImageViewer(url)`) with a list-detail scene at ≥ 600 dp. Detail (§10.3) with hero + shared element, info card + map thumbnail (§9.6), weekday group, meal sections and cards, allergen line, empty states, share, the image viewer (§10.4), meal long-press copy/share, click counting, predictive back.
**Done when:** screenshots of phone portrait, a tablet or landscape two-pane (resize the emulator or use a tablet AVD via `android emulator create medium_tablet`), dark mode, and the image viewer are reviewed. The back gesture works everywhere.

### Phase 5: Settings, allergens, language, onboarding
§10.5 and §10.6. Per-app language switching applies live and reloads the data in the new API language. Advanced resets work. About + licenses.
**Done when:** every settings row is functional, the onboarding shows on a fresh install only, the language switch to de/fr/it shows translated UI, and the screenshots are reviewed.

### Phase 6: Legi card, deep links, shortcuts, Quick Settings tile
§8 Legi details, the `ethmensa.ch/s/…` and `ethmensa://` deep links (including "not found" handling once the data is up to date), static shortcuts, the Quick Settings tile.
**Done when:** `adb shell am start -a android.intent.action.VIEW -d "ethmensa://s/eth/9"` opens that mensa; the https link opens it after enabling "Open by default"; the shortcuts and tile work; the Legi page loads with injected styling and full brightness, and brightness is restored on exit. (The user logs in with their own ETH account. Don't enter credentials yourself.)

### Phase 7: Notifications and location
Channels per sound, the reminder scheduler and receivers, the permission flows, geofencing (§8, §10.7). Clear our delivered notifications on resume.
**Done when:** with the time set 2 minutes ahead, a reminder arrives on the emulator on a weekday with the chosen sound; it reschedules after `adb reboot`; the geofence ENTER/EXIT behavior is checked with the emulator's location controls (`adb emu geo fix 8.5483 47.3774` → inside Zentrum; note **lng first** in `geo fix`); and denying a permission reverts the toggle.

### Phase 8: Polish, accessibility, performance, release build
- Go through the parity table (§7). Every row is done or explicitly documented as not ported.
- Accessibility: TalkBack labels, 48 dp touch targets, font scale 200 % screenshots, contrast on glass.
- Performance: baseline profile (optional), `jetpack-compose-performance` review, no main-thread I/O (`anr-audit-and-fix`), stable list keys (`mensa.id`), image sizes requested to match the thumbnails.
- `./gradlew lint` clean of errors. Fix the `MissingTranslation` warnings.
- Release: R8 on, keep rules checked (`r8-analyzer`), `assembleRelease` signed with a local git-ignored keystore (or debug keystore), APK size noted.
- Final screenshots: a gallery of 8–10 key screens in light and dark, saved under `android/docs/screenshots/` and committed.

**Done when:** the release APK runs on the emulator with no crashes in a full manual run-through, and lint and unit tests pass.

### Phase 9 (optional, ask the user first): extras
- **MensaZH-inspired extras from §5.7:** favorites and hidden mensas, the expanded list style, next-week menus, the diet filter, and "Open website".
- **Home-screen widget (Glance):** "Today at {most-clicked mensa}", showing the open state and the first 2–3 meals, refreshed every 3–6 h via WorkManager. Use the `glance-*` skills.
- **Wear OS app** (`:wear` module, `wear-compose-m3` skill): the watchOS feature set (list of mensas with menus today, detail menu, filter, appearance settings). See `watchos/` for the screens.
- **Material You icon variants, predictive back previews, baseline profile.**

---

## 14. Verification protocol (use it every phase)

```
# from android/
./gradlew :app:assembleDebug :app:testDebugUnitTest
android emulator start Pixel_API_37            # if not running; run it in the background, because it can keep the
                                               # shell attached. Readiness check: adb shell getprop sys.boot_completed → 1
./gradlew :app:installDebug                    # or: android run
adb shell am start -n io.github.mrghxst.ethmensa/.MainActivity
android screen capture --output=../.screens/<phase>-<name>.png   # verified flag; add -a to number UI elements
# then Read the PNG and critique it against §9/§10; fix; repeat
adb shell "cmd uimode night yes"   # dark mode check;  "night no" to revert
adb logcat -d -s AndroidRuntime:E  # crash check
```

- Keep scratch screenshots in `.screens/` (add it to `.gitignore`). Commit only the final gallery.
- Use `android layout` to inspect the UI tree when a tap target or semantics look wrong. Use `android input` to tap or scroll for flows.
- Compare against the iOS screenshots in `Assets/iPhone6.7-*.png` for information density and hierarchy. Match the content and hierarchy, not iOS chrome.

---

## 15. Known pitfalls

- **Windows path length:** keep temp work in short paths. Gradle's build dir under `android/app/build` is fine.
- **AGP 9 built-in Kotlin:** applying the old `kotlin-android` plugin fails the build. `kotlinOptions` moved, so use `kotlin { compilerOptions { … } }` or the AGP DSL. Follow the `agp-9-upgrade` skill.
- **material3 1.5.0 beta:** some expressive APIs need `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. Names may differ from blog posts, so check the sources.
- **Haze and Backdrop together:** don't nest two blur sources over the same content. Use Haze for bars and sheets and Backdrop for the dock and overlay buttons, each with its own source layer. Test scrolling performance on the API 36 emulator.
- **Notification channel sounds are immutable:** changing the sound means switching channels, not editing one.
- **Background location:** Android 11+ won't show the "Allow all the time" dialog; the user must pick it in Settings. Explain this before sending them there.
- **ETH data quirks:** price arrays can have 1–3 entries; meal-time names vary ("Lunchmenu", "Pranzo (Lunch)"); some rotas are valid until a date in the past for the next week. Always filter "valid today" and test with fixtures.
- **ZFV without a key** returns an empty success. Don't treat it as an error.
- **Locale vs API language:** de UI → `lang=de`; en, fr, it → `lang=en`. The cache must be keyed by language, or switching languages shows stale-language menus.
- **Time zone:** compute "today", weekdays and opening state in `Europe/Zurich`, not the device zone, because the menus are for Zürich. Show times as delivered.
- **WebView dark mode:** use `WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)` and rely on the injected CSS, or the Legi barcode may get inverted and become unscannable.
- **Brightness:** restore the previous `screenBrightness` (`BRIGHTNESS_OVERRIDE_NONE`) on every exit path, including process death and returning from the background.

---

## 16. Final report to the user

When finished (or blocked), report in plain language:
- What works, with the parity table summary (done / not ported and why).
- How to install it on their phone: enable developer options + USB debugging, then `adb install -r android/app/build/outputs/apk/release/app-release.apk`, or copy the APK and sideload it. Include the one-time "Open by default" step for `ethmensa.ch` links and the notification and location permission notes.
- Where the screenshots are, and any deviations from this prompt with reasons.
- The open optional items (Phase 9).
