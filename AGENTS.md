# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android application code, Jetpack Compose UI, C++/Chaquopy bits, Room `schemas/`, and Play Store assets under `src/main`.
- `core/`: Reusable libraries (`data`, `material-compat`, `ui`) for models, theming bridges, and shared Compose widgets.
- `compose/`: Small Compose utility modules (`persist`, `preferences`, `routing`, `reordering`) reused across features.
- `providers/`: Service integrations (`innertube`, `piped`, `kugou`, `lrclib`, `github`, `translate`, `sponsorblock`) built on Ktor; `common` holds shared contracts.
- `ktor-client-brotli/`: Brotli-enabled Ktor client pieces.
- `fastlane/`: Release metadata and screenshots; keep in sync when UI changes.
- Build logic lives in the root Gradle files; version catalogs are in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
Prereqs: JDK 23, Android SDK/Build Tools 36, NDK 28.2.13676358, CMake 3.22.1.  
Key commands:
```
./gradlew :app:assembleDebug              # Build debug APK (ABI splits)
./gradlew :app:installDebug               # Install on a connected device/emulator
./gradlew :app:bundleRelease              # Build release bundle; CI signing uses ANDROID_NIGHTLY_* vars
./gradlew detekt lint                     # Static analysis (Detekt + Android Lint)
./gradlew :app:testDebugUnitTest          # JVM/unit tests when present
./gradlew :app:connectedDebugAndroidTest  # Instrumentation tests on a device
```
Use `./gradlew clean` before release builds if native artifacts misbehave.

## Coding Style & Naming Conventions
- Kotlin official style with 4-space indents, LF line endings, max line length 100, trailing whitespace trimmed (`.editorconfig` enforced).
- Prefer explicit imports, no trailing commas; keep Compose functions small and hoist state.
- Names: `UpperCamelCase` for classes/composables, `lowerCamelCase` for vars/funcs, `UPPER_SNAKE_CASE` for constants; package names are lowercase and singular.
- Resources: `ic_*` for icons, `img_*` for images, `str_*` for strings; keep theming tokens in `core/material-compat` where possible.
- Run `./gradlew detekt` before pushing; follow `@formatter:off/on` only for necessary blocks.

## Testing Guidelines
- The snapshot has minimal tests; add new ones under `module/src/test/kotlin` (unit) and `module/src/androidTest/kotlin` (UI/instrumentation).
- Favor deterministic unit tests for view models, data mappers, and provider clients; mock Ktor with a test engine instead of hitting the network.
- Name tests `FeatureNameTest` or `FeatureNameKtTest`; keep one assertion focus per test where possible.
- Update `app/schemas` when Room entities change so migrations stay verifiable.

## Commit & Pull Request Guidelines
- Use short, imperative commits; prefer conventional-commit style with a scope, e.g., `feat(app): add offline cache toggle` or `fix(providers-innertube): guard null artist`.
- PRs should include a brief summary, linked issue (if any), test commands run, and screenshots/GIFs for UI changes.
- Call out new permissions, dependencies, or migration impacts; update fastlane metadata and changelogs when UX surfaces change.
- Keep PRs focused; avoid mixing refactors with feature work unless unavoidable.

## Security & Configuration Tips
- Do not commit secrets; signing creds come from environment (`ANDROID_NIGHTLY_KEYSTORE*`). Keep `local.properties` limited to SDK paths.
- Native/Chaquopy pieces fetch Python deps (`yt-dlp`); ensure builders have internet for first-time sync, or pre-cache if running offline CI.
