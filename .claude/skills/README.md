# Project skills (Android port)

Claude Code loads every folder here as a project skill. They were added on 2026-10-28 for the Android port described in `ANDROID_PORT_PROMPT.md`.

## Official Google Android skills (Apache-2.0, github.com/android/skills)

Installed with the Android CLI: `android skills add <name> --agent=claude-code --project=.`

| Skill | Use it for |
|---|---|
| `android-cli` | The `android` CLI: create, build, run, emulators, screenshots, UI layout tree, docs search |
| `navigation-3` | Navigation 3 back stack, deep links, list-detail scenes |
| `navigation-event` | Predictive back |
| `adaptive` | Phone, tablet and foldable layouts, two-pane list-detail |
| `edge-to-edge` | Insets, system bars, IME |
| `glance-canonical-layouts`, `glance-appwidget-updates-and-memory` | Home screen widget (optional phase) |
| `jetpack-compose-performance` | Recomposition and jank checks |
| `testing-setup` | Unit, UI and screenshot test setup |
| `android-permissions-security`, `android-intent-security` | Notification and location permissions, exported components, deep links |
| `agp-9-upgrade` | AGP 9 build script conventions |
| `r8-analyzer` | Release shrinking and keep rules |
| `anr-audit-and-fix` | Main-thread work audit |
| `play-policy-insights` | Data safety and permission declarations, if ever published |
| `wear-compose-m3` | Optional Wear OS phase (port of the watchOS app) |

Update them with `android skills update --all`. To see every official skill, run `android skills list`.

## Chris Banes' Kotlin and Compose skills (Apache-2.0, github.com/chrisbanes/skills, commit 8fc8836)

`compose-animations`, `compose-component-design`, `compose-state-and-effects`, `compose-performance`, `compose-focus-navigation`, `compose-ui-testing-patterns`, `kotlin-concurrency-and-flow`, `kotlin-api-design`, `kotlin-control-flow`, `gradle-run`.

These were copied as-is, with their `LICENSE`. They link to each other by relative path, so keep the folder names. To refresh them, copy the same folders from a fresh clone of the repository.
