# Extra Dim

A simple Android app that reduces screen brightness below the system's default minimum limit.

## Project overview

- Purpose: let the user dim the screen further than Android's system minimum
  brightness, for reading in dark rooms / reducing eye strain at night.
- Language: **Kotlin** (native Android app, Jetpack Compose).

## Android app architecture & conventions

Layout (single-module Gradle project — the `:app` module):

- Native **Kotlin + Jetpack Compose** (Material 3), organized
  **package-by-feature** under `app/src/main/java/com/extradim/`:
  `data/` (persistence / repository), `di/` (manual DI container),
  `dim/` (overlay controller + foreground service),
  `quick/` (Quick Settings tile + transparent toggle activity), `ui/`
  (Compose screen, `MainViewModel`, `theme/`), plus `ExtraDimApp` /
  `MainActivity` at the package root.
- **On-device architecture** (no backend at runtime): the UI talks to persisted
  state through `SettingsRepository` (DataStore) and drives the overlay via
  `DimController` / `DimService` obtained from the `AppContainer`. The container
  is the single wiring point — layers don't reach into each other's framework
  types directly (e.g. `ui` touches `dim` only through the container).

Stack & tooling:

- Stack: Jetpack Compose (Material 3), DataStore (settings). This is a
  single-page app — deliberately **no** Hilt/KSP, Navigation Compose, or
  kotlinx.serialization; DI is a minimal manual `AppContainer` held by
  `ExtraDimApp`. (The generic stack list below reflects the Money Manager
  convention; this repo's actual build uses only what's in
  `android/gradle/libs.versions.toml`.)
- Versions are centralized in `android/gradle/libs.versions.toml`. Pins: AGP
  **9.3.0**, Gradle **9.5.0**, Kotlin **2.4.10**, KSP **2.3.11** (KSP uses
  *decoupled* versioning — not `<kotlin>-<ksp>`), compileSdk/targetSdk **37**,
  minSdk **26**, JVM target **17**.
- AGP 9 provides **built-in Kotlin**: do **not** apply the
  `org.jetbrains.kotlin.android` plugin (it errors). The compose, serialization,
  and KSP plugins still apply on top; Kotlin compiler options go in the
  `kotlin { compilerOptions { } }` DSL (jvmTarget defaults to
  `compileOptions.targetCompatibility`).
- **Dynamic color** (Material You) only on API 31+ — guard with
  `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`, else fall back to the static
  scheme (crashes on 26–30 without the guard).

## Android environment

- Built **SDK-only, without Android Studio** — the Android command-line tools +
  a JDK, driven from VS Code / a terminal, deployed to a **physical device**
  over USB/Wi-Fi debugging (no emulator).
- Toolchain on the dev PC: a **JDK** (via `JAVA_HOME`) and the **Android SDK**
  (via `ANDROID_HOME` / `ANDROID_SDK_ROOT`), with `cmdline-tools\latest\bin` and
  `platform-tools` on `PATH`.
- The build's JVM **target** is 17 (AGP 9.3 baseline); the JDK that *runs*
  Gradle may be newer.
- Build & run **natively on Windows** (no WSL/Docker): `./gradlew installDebug`
  from `android/` builds and installs to the connected phone.
- `android/local.properties` (holds `sdk.dir`) is **machine-local and
  gitignored** — never commit it. Every other `android/` config is committed.
- Gradle runs via the wrapper (`./gradlew` from `android/`). The wrapper files
  (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` +
  `.properties`) are **committed** — clone and run, no `gradle wrapper` step.

## Current status (2026-08-27) — implemented & merged

The full app is implemented and merged to `main` via PR #2. It builds with
`./gradlew installDebug` and installs/runs on a physical device (tested on a
OnePlus / Android 16, API 36, with `adb` driving the tile via
`cmd statusbar click-tile`).

### What's implemented

- **One-page Compose UI** (`MainActivity` + `MainScreen`, `ui/MainViewModel`):
  a brightness slider (`1 - dimLevel`, default ~40% brightness so dim is visible
  on first toggle) and a "Dimming" on/off switch. The ViewModel exposes
  `dimLevel` / `enabled` as `StateFlow`s so the UI stays in sync with the tile.
- **DataStore persistence** (`data/SettingsRepository`): stores the dim level
  (`dim_level`, 0..1) and the enabled flag (`enabled`); the slider position and
  on/off state survive relaunch. The user's slider value is preserved across
  toggles (only the initial default seeds a visible dim).
- **Overlay dimming** (`dim/DimController` + `dim/DimService`): a full-screen
  black overlay window drawn by a `specialUse` foreground service. `DimService`
  applies the persisted level on `START` / `UPDATE` and self-stops when
  `enabled` flips off. Full-screen coverage incl. display cutout (pure black at
  the darkest setting).
- **Overlay-only dimming to preserve auto-brightness:** Dimming is purely
  overlay-based without touching system brightness (`WRITE_SETTINGS` is not
  used), so device auto-brightness is not disturbed. Full-screen coverage
  includes the navigation bar and display cutout.
- **Quick Settings tile** (`quick/QuickDimTileService` + `quick/ToggleActivity`):
  tap toggles dim on/off. On Android 15+ (API 35+) the service is started
  **directly from the tile tap** (foreground-initiated), so the tile does not
  open the app; older Android falls back to the transparent `ToggleActivity`.
  Long-press opens the app via `QS_TILE_PREFERENCES`. The app offers an
  **"Add to Quick Settings"** button (`StatusBarManager.requestAddTileService`,
  API 33+) to install the tile.
- **Manual DI** via `ExtraDimApp` → `AppContainer` (no Hilt/KSP for simplicity).

### Key design notes / why it's built this way

- **Dimming below the system minimum** requires `SYSTEM_ALERT_WINDOW` and a
  translucent overlay; the OS brightness cannot be clamped below its min.
- **QS tile on Android 12–14:** starting a foreground service from a tile tap
  was forbidden, so the transparent `ToggleActivity` was the workaround. **On
  Android 15+** a tile tap is a valid foreground initiation, so
  `QuickDimTileService` now calls `DimService.start()` directly and only falls
  back to the activity on older versions / if rejected. This is why the tile no
  longer opens the app.
- **Brightness value is preserved:** toggling on/off never overwrites the user's
  slider position; only the initial default (`SettingsRepository.DEFAULT_DIM_LEVEL
  = 0.6`) seeds a visible dim.
- **`getApplication()` in `AndroidViewModel`:** on this Compose stack it's
  `getApplication<ExtraDimApp>()`; `application` is private. `ExtraDimApp` holds
  `container: AppContainer`, so access is `app.container.settingsRepository` /
  `app.container.dimController` (the container is a property, not a receiver).

### Known limitations / next steps

1. **Tile must be added by the user** via the in-app "Add to Quick Settings"
   button (or the QS editor); it is not auto-added.
2. **Overlay permission** (`SYSTEM_ALERT_WINDOW`) must be granted by the user;
   the app prompts for it.
3. **POST_NOTIFICATIONS** is optional; if not granted the FGS notification is
   suppressed (the overlay still works).
4. `local.properties` (`sdk.dir`) is machine-local / gitignored — created
   locally for the build, never commit.

## Workflow & git

- **I review every change.** After you make a change, stop and let me review it.
- **Do NOT commit or push** unless I explicitly tell you to. Only after I say
  "commit" / "push" may you run those git commands.
- **Modular commits.** When I ask you to commit, do NOT dump everything into one
  commit. Split the changes into reasonable, logically-grouped commits (e.g.
  restructure vs. feature vs. docs) each with its own clear message.

### Branching & PR flow

- Every new feature starts on a **feature branch created from `main`**. Do the
  development there.
- Before creating a new branch, **fetch the latest `main`** and branch from it
  (e.g. `git fetch origin && git checkout -b <branch> origin/main`) so the
  branch always starts from up-to-date `main`.
- Only after the feature is **well tested and working** does it go to `main`
  via a **PR review**.
- **No local merge to `main`, and no direct push to `main`.** `main` is updated
  exclusively through the PR review process.
