# ETH Mensa: iOS app plus an Android port in progress

This is a private fork (github.com/mrghxst/ethmensa) of the GPLv3 ETH Mensa iOS app by Alexandre Reol (github.com/alexandrereol/ethmensa).
The Swift sources in `ethmensa/`, `watchos/`, `widget/` and `clip/` are the reference implementation. Don't change them.
The Android port lives in `android/` and follows `ANDROID_PORT_PROMPT.md`. Read that file before doing any Android work.

## Machine setup (Windows 11, already installed on 2026-10-28)

- JDK: Eclipse Temurin 21 at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (`JAVA_HOME` is set machine-wide)
- Android SDK: `C:\Users\ghst\AppData\Local\Android\Sdk` (`ANDROID_HOME` is set for the user). Installed: platform-tools, emulator, cmdline-tools/latest, platforms android-36 and android-37.0, build-tools 37.0.0, Google Play x86_64 system images for API 36 and 37.0
- Android CLI: `android` (winget `Google.AndroidCLI`, 1.1.x). Metrics are off via `%USERPROFILE%\.androidrc`
- Android Studio 2026.2.1 "Rabbit" at `C:\Program Files\Android\Android Studio`
- Emulators: `Pixel_API_37` (Pixel 9, API 37) and `medium_phone` (API 36). WHPX acceleration works.
- `.claude/settings.local.json` sets `JAVA_HOME` and `ANDROID_HOME` for Claude Code shells, because the app may have started before those variables existed.
- If `android`, `adb` or `emulator` are not on `PATH` in a shell, use the full paths: `%LOCALAPPDATA%\Microsoft\WinGet\Links\android.exe`, `%ANDROID_HOME%\platform-tools\adb.exe` and `%ANDROID_HOME%\emulator\emulator.exe`.
- The npm-installed `claude` CLI on this machine is broken ("native binary not installed"). Don't rely on `claude plugin ...` from a shell.
- Windows paths over 260 characters break git and the JDK. Keep temp work in short paths such as `C:\Users\ghst\AppData\Local\Temp\<name>`.

## Android conventions

- Gradle project root is `android/`. Run `./gradlew` from there (`gradlew.bat` in PowerShell).
- Secrets go in `android/local.properties` (git-ignored), never in the repo. `ZFV_API_KEY` (the shared public ZFV key, also listed in `ANDROID_PORT_PROMPT.md` §5.3) enables the UZH mensas.
- Verify UI on the emulator with `android run` / `./gradlew installDebug`, then `android screen capture` and read the PNG.
- Project skills for Android, Compose and Kotlin are in `.claude/skills/` (see its README).
