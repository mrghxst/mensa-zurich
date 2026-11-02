# Mensa Zürich

Unofficial Android app (Kotlin, Jetpack Compose) for the ETH Zürich and UZH mensa menus. It is a GPLv3 port of ETH Mensa for iOS by Alexandre Reol (github.com/alexandrereol/ethmensa), which is the behavioral reference. `docs/port-spec.md` is the specification the port was built from, and `docs/PARITY.md` tracks parity with the iOS app.

## Layout

- `app/` phone app, `wear/` Wear OS app, `core/` shared domain and data layer (with its unit tests), `baselineprofile/` startup profile generator.
- `tools/` Python scripts that generate resources. Generated files say so in a header comment; edit the script, not the output:
  - `xcstrings_to_android.py` turns `tools/ios/Localizable.xcstrings` (the iOS app's translations) into `app/src/main/res/values*/strings.xml`.
  - `android_strings.py` writes the Android-only strings (`strings_android.xml`), `wear_strings.py` copies the watch's subset.
  - `make_icons.py` and `make_shortcut_icons.py` draw the icons from Lucide vectors.
- `site/` is the website with the privacy policy and terms (GitHub Pages, published by `tools/publish_site.py` to the public repo mrghxst/mensa). The app links to its `/privacy/` and `/terms/` URLs.

## Conventions

- Run Gradle from the repository root: `./gradlew test lint assembleDebug`.
- Secrets and signing config live in `local.properties` (git-ignored), never in the repo. `ZFV_API_KEY` overrides the shared public ZFV key in `build.gradle.kts`. Never print or commit the `RELEASE_*` passwords.
- The app and watch versions come from `appVersionCode` and `appVersionName` in the root `build.gradle.kts`. Releases follow the README's "Releasing" section; the tag is `v<appVersionName>`.
- Don't add links to ethmensa.ch or the iOS author's contacts. Incoming ethmensa.ch share links are still handled for interop with the iOS app.
- Verify UI changes on an emulator in light and dark mode before committing. Commits use conventional messages (`feat:`, `fix:`, `docs:` …).
