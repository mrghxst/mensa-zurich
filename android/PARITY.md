# Feature parity with ETH Mensa for iOS

Checklist from `ANDROID_PORT_PROMPT.md` §7. `[x]` done, `[~]` partly done (see note), `[ ]` open.

| # | iOS feature | Android implementation | Status |
|---|---|---|---|
| 1 | ETH and UZH mensas with weekly menus, cached then fresh | `MensaRepository` flow, `WeeklyJsonCache` | [x] data layer |
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
| 17 | Open shared links | Intent filters + `ethmensa://` | [ ] |
| 18 | Legi card | `LegiScreen` | [ ] |
| 19 | Legi from Control Center | Quick Settings tile + app shortcut | [ ] |
| 20 | Home-screen quick actions | Static launcher shortcuts | [ ] |
| 21 | Siri, Shortcuts, Spotlight | Covered by app shortcuts | [ ] |
| 22 | Onboarding welcome | `OnboardingScreen`, shown once, two panes on tablets | [x] |
| 23 | Settings | Settings screens on DataStore, reset actions with confirmation | [x] notifications row wired in Phase 7 |
| 24 | In-app language en/de/fr/it | Per-app language (AppCompat) + picker, `locales_config.xml` | [x] |
| 25 | Allergen settings | Settings > Allergens, allergen-friendly chip | [x] |
| 26 | Notifications | Alarm reminder + geofences | [ ] |
| 27 | Opening the app clears delivered reminders | `cancelAll` on resume | [ ] |
| 28 | Offline state | Banner + full-screen state | [x] |
| 29 | Settings sync over iCloud | Android Auto Backup of DataStore (caches excluded) | [x] |
| 30 | Tablet split view | Navigation 3 list-detail scene | [x] |
| 31 | Dark mode | Theme + System/Light/Dark setting, optional wallpaper colors | [x] |
| 32 | Watch app | Optional Phase 9 | — |
| 32a | (Android extra) Nutrition values | `Nutrition` on `Meal`, collapsible row in the meal card | [x] |
| 32b | (Android extra) Placeholder meals hidden | `MealText.isPlaceholder` | [x] |
| 33 | Widget | Optional Phase 9 | — |
| — | Not ported | See below | |

## Notes

- The filter sheet uses an opaque background instead of glass: a `ModalBottomSheet` draws in its own window, so neither Haze nor Backdrop can sample the list behind it.

- The map thumbnail is the §9.6 fallback: MapLibre Compose 0.20.0 crashed on Android with `UnsatisfiedLinkError: jniMaplibreNativeC`, so the tile shows a pin and opens a maps app at the bundled coordinates. No map tiles are downloaded.

## Not ported

Filled in during Phase 8.
