# Feature parity with ETH Mensa for iOS

Checklist from `ANDROID_PORT_PROMPT.md` §7. `[x]` done, `[~]` partly done (see note), `[ ]` open.

| # | iOS feature | Android implementation | Status |
|---|---|---|---|
| 1 | ETH and UZH mensas with weekly menus, cached then fresh | `MensaRepository` flow, `WeeklyJsonCache` | [x] |
| 2 | Three list cell styles | `MensaRow` variants + Layout option in the filter sheet | [x] |
| 3 | Open/closed status dot | `StatusDot` with a TalkBack label | [x] |
| 4 | Search by mensa name | Search field in the floating dock | [x] |
| 5 | Filter menu | Filter bottom sheet | [x] sheet is opaque (see note) |
| 6 | Active filter chips, tap to remove | Chip row under the title | [x] |
| 7 | Smart sorting by open count | `ClickCountStore` + pipeline sort | [x] |
| 8 | Pull to refresh, skeleton loading, empty state | `PullToRefreshBox`, shimmer skeleton, empty state | [x] |
| 9 | Long-press a mensa: Open, Share | Long-press menu | [x] |
| 10 | Detail: map thumbnail, address, hours, open in maps | Map tile + `geo:` intent with a chooser | [x] static pin tile (see note) |
| 11 | Detail: weekday picker | Connected 7-day button group | [x] |
| 12 | Detail: meal sections and cells | `MealCard` | [x] |
| 13 | Meal photo fullscreen zoom | telephoto zoom + flick to dismiss | [x] |
| 14 | Long-press a meal: Copy, Share | Bottom sheet (Copy, Share) | [x] |
| 15 | No-menu and no-allergen-friendly-meal states | Empty states in the detail | [x] |
| 16 | Share mensa link | Sharesheet | [x] |
| 17 | Open shared links | `AppLink` parser, https filter (user opt-in) + `ethmensa://`, not-found dialog | [x] |
| 18 | Legi card | `LegiScreen` WebView, CSS on the card page only, brightness 1.0 + keep screen on | [x] card page itself not seen (needs a login) |
| 19 | Legi from Control Center | `LegiTileService` + `legi` app shortcut | [x] |
| 20 | Home-screen quick actions | `res/xml/shortcuts.xml` (Legi first: Pixel Launcher shows four) | [x] |
| 21 | Siri, Shortcuts, Spotlight | Covered by app shortcuts | [x] |
| 22 | Onboarding welcome | `OnboardingScreen`, shown once, two panes on tablets | [x] |
| 23 | Settings | Settings screens on DataStore, reset actions with confirmation | [x] notifications row wired in Phase 7 |
| 24 | In-app language en/de/fr/it | Per-app language (AppCompat) + picker, `locales_config.xml` | [x] |
| 25 | Allergen settings | Settings > Allergens, allergen-friendly chip | [x] |
| 26 | Notifications | Channel per sound, `AlarmManager.setWindow` weekday reminder, boot/time/update receivers, campus geofences | [x] |
| 27 | Opening the app clears delivered reminders | `Reminders.clearDelivered` in `MainActivity.onResume` | [x] |
| 28 | Offline state | Banner + full-screen state | [x] |
| 29 | Settings sync over iCloud | Android Auto Backup of DataStore (caches excluded) | [x] |
| 30 | Tablet split view | Navigation 3 list-detail scene | [x] |
| 31 | Dark mode | Theme + System/Light/Dark setting, optional wallpaper colors | [x] |
| 32 | Watch app | Optional Phase 9 | — |
| 32a | (Android extra) Nutrition values | `Nutrition` on `Meal`, collapsible row in the meal card | [x] |
| 32b | (Android extra) Placeholder meals hidden | `MealText.isPlaceholder` | [x] |
| 32c | (Android extra, Phase 9) Favorites and hidden mensas | Long-press menu, favorites pinned on top, Settings > Favorites and hidden | [x] |
| 32d | (Android extra, Phase 9) Diet filter | Vegetarian / vegan in the filter sheet, applied to the list and the detail | [x] |
| 32e | (Android extra, Phase 9) "Menus" list style | Fourth cell style with the day's meals and prices under each mensa | [x] |
| 32f | (Android extra, Phase 9) Next week | This week / next week switch in the detail: ETH from the two-week rotas, UZH with `week(week: "next")` | [x] ETH publishes next week late in the week; until then its dishes are placeholders and are dropped |
| 32g | (Android extra, Phase 9) Open website | Glass button in the detail header, from the ETH directory `web` field | [x] |
| 33 | Widget | Optional Phase 9 | — |
| — | Not ported | See below | |

## Notes

- The filter sheet uses an opaque background instead of glass: a `ModalBottomSheet` draws in its own window, so neither Haze nor Backdrop can sample the list behind it.

- The map thumbnail is the §9.6 fallback: MapLibre Compose 0.20.0 crashed on Android with `UnsatisfiedLinkError: jniMaplibreNativeC`, so the tile shows a pin and opens a maps app at the bundled coordinates. No map tiles are downloaded.

- Legi: the ETH login (Keycloak) lives on `eduapp.ethz.ch/keycloak/`, the same host as the card. Our CSS applies only outside `/keycloak/`, and non-card pages get a white WebView background, so the login form stays readable in dark mode.

- Shortcuts: Android allows more, but the Pixel Launcher shows four static shortcuts. The Legi card is first, so Irchel is the one hidden there.

- Location-based reminders use all three campuses (iOS: Zentrum and Hönggerberg) and pass latitude and longitude in the right order (iOS swaps them in `GeofencingManager.makeGeoRegion`).

- Reminders use an inexact 10-minute window, so no exact-alarm permission is needed. The Android-only footer under the time says so.

- Release build: R8 needs `-keep class androidx.core.splashscreen.**` (core-splashscreen 1.2.0). Without it the system splash window stays on top on API 37 while the app runs underneath.

- Accessibility: TalkBack labels on all icon-only controls, touch targets of at least 48 dp, and rows that wrap their value or price under the title at 200 % font size.

## Not ported

| iOS feature | Why |
|---|---|
| App Clip | No Android equivalent for a private sideloaded app (Instant Apps are retired). |
| SharedWithYou | Apple-only framework. Shared links open the app through the `ethmensa.ch` and `ethmensa://` links instead. |
| Handoff | Apple-only (continuity between Apple devices). |
| "What's new" sheet | Tied to App Store releases; a private build has no release notes to show. |
| Version gate (`api.ethmensa.ch/v1/app/version`) | Compares iOS build numbers (§5.5), so it doesn't apply to this build. |
| App Store review and share-the-app prompts | No store listing. |
| ETH Video promotion | An iOS promo row unrelated to menus. |
| WhatsApp and email support | Those contacts belong to the iOS app's maintainer; this unofficial port must not route its users there. |
| Siri, Shortcuts, Spotlight | Covered by the static app shortcuts and the Quick Settings tile. |
| Map tiles (MapLibre + OpenFreeMap) | MapLibre Compose 0.20.0 fails to load its native library, so the detail shows the §9.6 static pin that opens a maps app. |
| Watch app, widget | Optional Phase 9. |

## Screenshots

`docs/screenshots/` holds the release build in light and dark: onboarding, list, detail, photo viewer, filters, settings, notifications and the Legi card (login page).
