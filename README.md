# mMusic
Lightweight Android music app with YouTube Music playback, local library, and offline caching.

## Features
- Play nearly any YouTube Music track or video; open `watch`, `playlist`, `channel` links directly.
- Local library playback with background play and audio normalization.
- Offline caching for tracks and playlists.
- Search across songs, albums, artists, videos, and playlists; discover by mood/genre.
- Lyrics: fetch, display, and edit synced lyrics.
- Playlist import/export (YouTube) plus local/cloud playlist management.
- Android Auto support and highly configurable theming (Material You).

## Quick start
Prereqs: JDK 23, Android SDK/Build Tools 36, NDK 28.2.13676358, CMake 3.22.1.

Commands:
- `./gradlew :app:assembleDebug` — build debug APK (ABI splits).
- `./gradlew :app:installDebug` — install on a device/emulator.
- `./gradlew detekt lint` — static analysis.
- `./gradlew :app:testDebugUnitTest` — JVM tests (when present).
- `./gradlew :app:connectedDebugAndroidTest` — instrumentation tests.

If native artifacts misbehave before release builds, run `./gradlew clean`.

## Modules
- `app/`: Android app (Compose UI, playback service, Room schemas, Play assets).
- `core/`: Shared UI, theming bridge, and data models.
- `compose/`: Reusable Compose utilities (persist, preferences, routing, reordering).
- `providers/`: External services (Innertube, Piped, Kugou, LrcLib, GitHub, translate, SponsorBlock) built on Ktor; `common` holds shared contracts.
- `ktor-client-brotli/`: Brotli-enabled Ktor client pieces.
- `fastlane/`: Release metadata and screenshots.

See `docs/ARCHITECTURE.md` for data flow, playback pipeline, and provider details.

## Development notes
- Run `./gradlew detekt` before pushing.
- Keep Room schemas under `app/schemas` up to date after entity changes.
- Avoid committing secrets; signing comes from `ANDROID_NIGHTLY_*` env vars. `local.properties` should only hold SDK paths.

## Acknowledgments
- [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients)
- [ionicons](https://github.com/ionic-team/ionicons)
- [Flaticon: Ilham Fitrotul Hayat](https://www.flaticon.com/authors/ilham-fitrotul-hayat) for the logo base icon.

## Disclaimer
This project is not affiliated with YouTube, Google LLC, or their affiliates. All trademarks are owned by their respective owners.
