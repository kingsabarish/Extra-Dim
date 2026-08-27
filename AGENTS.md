# Extra Dim

A simple Android app that reduces screen brightness below the system's default minimum limit.

## Project overview

- Purpose: let the user dim the screen further than Android's system minimum
  brightness, for reading in dark rooms / reducing eye strain at night.
- Language: **Kotlin** (native Android app, Jetpack Compose).

## Android app architecture & conventions

Layout (single-module Gradle project — the `:app` module):

- Native **Kotlin + Jetpack Compose** (Material 3), organized
  **package-by-feature** under `app/src/main/java/<package>/`:
  `data/`, `domain/{model,repository}`, `ui/{theme,components,navigation,feature/*}`,
  `di/`. Empty layers are held by `.gitkeep` until filled in.
- **On-device architecture** (no backend at runtime): the UI depends only on
  repository **interfaces** in `domain/repository/`; implementations map
  persistence ↔ domain models. `domain/**` has no Android framework imports;
  `ui/**` never imports `data/**`. Errors cross the boundary as a sealed
  `AppResult`, not exceptions.

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

## Current status (2026-08-27) — work in progress, paused

Development and device testing are currently **paused** (per user request). The
project scaffolding + feature code is built and installs, but is **not yet
committed** (all work is on the local `feature/init-app-project` branch; only
the AGENTS.md + README that came through the earlier PR are on `main`).

### What's implemented (uncommitted on `feature/init-app-project`)

- **One-page Compose UI** (`MainActivity` + `MainScreen`, `ui/MainViewModel`):
  a brightness slider (full by default; pulling it down increases the dim
  overlay) and a "Dimming" on/off switch.
- **DataStore persistence** (`data/SettingsRepository`): stores the dim level
  (`dim_level`, 0..1) and the enabled flag (`enabled`), so the slider position
  and on/off state survive relaunch.
- **Overlay dimming** (`dim/DimController` + `dim/DimService`): a full-screen
  black overlay window drawn by a foreground service. It sits on top of the
  system brightness, so it works in combination with the system brightness
  control. `DimService` applies the persisted level on both `START` and
  `UPDATE`. Full-screen coverage incl. display cutout (pure black at the
  darkest setting).
- **Quick Settings tile** (`quick/QuickDimTileService` + `quick/ToggleActivity`):
  tap toggles dim, long-press opens the app (via the
  `android.service.quicksettings.action.QS_TILE_PREFERENCES` intent filter on
  `MainActivity`). The manifest's `ToggleActivity` is a transparent,
  immediately-finishing activity used to apply the toggle from a **foreground
  context**.
- **Manual DI** via `ExtraDimApp` → `AppContainer` (no Hilt/KSP for simplicity).

### Key design notes / why it's built this way

- **Dimming below the system minimum** requires the `SYSTEM_ALERT_WINDOW`
  ("Display over other apps") permission and a translucent overlay; you cannot
  clamp the OS brightness below its min. User confirmed this approach.
- **Proven bug:** on Android 12+ (target device is Android 16), starting a
  foreground service directly from a QS tile tap throws
  `ForegroundServiceStartNotAllowedException` (background context). Fix: the
  tile only flips the persisted flag and launches `ToggleActivity`
  (`startActivityAndCollapse`), which is foreground and can start `DimService`.
  Uses the `PendingIntent` overload of `startActivityAndCollapse` on API 34+.
- **Scope-lifecycle bug fixed:** `QuickDimTileService` previously cancelled its
  shared `CoroutineScope` in `onStopListening`, so later `onClick` toggles
  launched into a cancelled scope and silently no-op'd. Now uses a fresh scope
  per listening session and a throwaway scope in `doToggle`.
- **`getApplication()` in `AndroidViewModel`:** on this Compose stack it's
  `getApplication<ExtraDimApp>()`; `application` is private. `ExtraDimApp`
  holds `container: AppContainer`, so access is `app.container.settingsRepository`
  / `app.container.dimController` (the container is a property, not a receiver).

### Known issues / next steps (when development resumes)

1. **Tile not yet added on the device** — `dumpsys` shows no registered QS tile
   for `com.extradim`. The user could not test the tile because it wasn't in the
   panel. Next step: add an "Add Quick Settings tile" button using
   `TileService.requestAddTileService(...)` (Android 13+).
2. **POST_NOTIFICATIONS is not granted** on the device -> the FGS notification
   won't show (harmless to the overlay, but the ongoing char notification is
   expected). May want to request it via the app.
3. Slider-then-enable, minimum-darkness, and the tile toggle were **not fully
   verified visually on-device** before pause (couldn't auto-grant the overlay
   appop via adb; needs user grant or on-device test).
4. `local.properties` (`sdk.dir`) is machine-local / gitignored — created
   locally for the build, never commit.
5. Nothing on `feature/init-app-project` is committed yet — needs a modular
   commit + PR (per workflow) when the user resumes and approves.

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
