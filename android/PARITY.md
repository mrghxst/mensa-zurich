# Feature parity with ETH Mensa for iOS

Checklist from `ANDROID_PORT_PROMPT.md` §7. `[x]` done, `[~]` partly done (see note), `[ ]` open.

| # | iOS feature | Android implementation | Status |
|---|---|---|---|
| 1 | ETH and UZH mensas with weekly menus, cached then fresh | `MensaRepository` flow, `WeeklyJsonCache` | [x] data layer |
| 2 | Three list cell styles | `MensaRow` variants + Layout option in the filter sheet | [ ] |
| 3 | Open/closed status dot | `StatusDot` with a TalkBack label | [ ] |
| 4 | Search by mensa name | Search field in the floating dock | [~] rule in `MensaListPipeline` |
| 5 | Filter menu | Glass bottom sheet | [~] rules in `Filters.kt` |
| 6 | Active filter chips, tap to remove | Chip row under the title | [~] `ListFilters.activeFilters` |
| 7 | Smart sorting by open count | `ClickCountStore` + pipeline sort | [~] data + rule |
| 8 | Pull to refresh, skeleton loading, empty state | `PullToRefreshBox`, shimmer skeleton, empty state | [ ] |
| 9 | Long-press a mensa: Open, Share | Long-press menu | [ ] |
| 10 | Detail: map thumbnail, address, hours, open in maps | MapLibre thumbnail + `geo:` intent | [~] coordinates bundled |
| 11 | Detail: weekday picker | Connected 7-day button group | [ ] |
| 12 | Detail: meal sections and cells | `MealCard` | [ ] |
| 13 | Meal photo fullscreen zoom | telephoto | [ ] |
| 14 | Long-press a meal: Copy, Share | Bottom sheet | [ ] |
| 15 | No-menu and no-allergen-friendly-meal states | Empty states in the detail | [ ] |
| 16 | Share mensa link | Sharesheet | [ ] |
| 17 | Open shared links | Intent filters + `ethmensa://` | [ ] |
| 18 | Legi card | `LegiScreen` | [ ] |
| 19 | Legi from Control Center | Quick Settings tile + app shortcut | [ ] |
| 20 | Home-screen quick actions | Static launcher shortcuts | [ ] |
| 21 | Siri, Shortcuts, Spotlight | Covered by app shortcuts | [ ] |
| 22 | Onboarding welcome | `OnboardingScreen` | [ ] |
| 23 | Settings | Settings screens | [~] `SettingsRepository` |
| 24 | In-app language en/de/fr/it | Per-app language + picker | [~] strings generated, `locales_config.xml` |
| 25 | Allergen settings | Settings > Allergens | [~] allergen rules |
| 26 | Notifications | Alarm reminder + geofences | [ ] |
| 27 | Opening the app clears delivered reminders | `cancelAll` on resume | [ ] |
| 28 | Offline state | Banner + full-screen state | [~] `Connectivity` |
| 29 | Settings sync over iCloud | Android Auto Backup | [~] backup rules |
| 30 | Tablet split view | Navigation 3 list-detail scene | [ ] |
| 31 | Dark mode | Theme + override setting | [~] theme |
| 32 | Watch app | Optional Phase 9 | — |
| 32a | (Android extra) Nutrition values | `Nutrition` on `Meal` | [~] data |
| 32b | (Android extra) Placeholder meals hidden | `MealText.isPlaceholder` | [x] |
| 33 | Widget | Optional Phase 9 | — |
| — | Not ported | See below | |

## Not ported

Filled in during Phase 8.
