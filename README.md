
# mMusic
A friendly music app for Android. It plays your YouTube Music favorites, mixes in your local library, and lets you keep tracks handy offline. The experience is meant to feel light, fast, and personal.

Heavily inspired by and based on the wonderful **Vitune** by Bart Ostooven. Huge thanks to Bart for paving the way.

## What it does
- Play most YouTube Music songs, albums, and playlists (drop in a watch/playlist/channel link and go).
- Keep a local library with background play and optional loudness normalization.
- Save music for offline listening.
- Search across songs, albums, artists, videos, and playlists; browse moods and genres.
- View and edit synced lyrics.
- Import/export playlists (YouTube), and manage local playlists.
- Android Auto support and flexible theming (Material You).

## Getting started
You need JDK 23, Android SDK/Build Tools 36, NDK 28.2.13676358, and CMake 3.22.1.

Handy commands:
- `./gradlew :app:assembleDebug` – build a debug APK (ABI splits).
- `./gradlew :app:installDebug` – install on a connected device/emulator.
- `./gradlew detekt lint` – quick static checks.
- `./gradlew :app:testDebugUnitTest` – JVM tests when available.
- `./gradlew :app:connectedDebugAndroidTest` – instrumentation tests on a device.

If native bits act up, run `./gradlew clean` before building a release.

## Project layout (short version)
- `app/` – Android app (Compose UI, playback service, Room schemas, Play assets).
- `core/` – Shared UI, theming bridge, and data models.
- `compose/` – Small Compose helpers (persist, preferences, routing, reordering).
- `providers/` – Service integrations (Innertube, Piped, Kugou, LrcLib, GitHub, translate, SponsorBlock) on Ktor.
- `fastlane/` – Release metadata and screenshots.

More detail lives in `docs/ARCHITECTURE.md`.

## Developer notes
- Run `./gradlew detekt` before pushing.
- Keep Room schemas under `app/schemas` in sync after entity changes.
- Do not commit secrets; signing uses `ANDROID_NIGHTLY_*` env vars. `local.properties` should only hold SDK paths.

## Acknowledgments
- Vitune by Bart Ostooven (foundation and inspiration).
- [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients)
- [ionicons](https://github.com/ionic-team/ionicons)
- [Flaticon: Ilham Fitrotul Hayat](https://www.flaticon.com/authors/ilham-fitrotul-hayat) for the logo base icon.

## Disclaimer
This project is not affiliated with YouTube, Google LLC, or their affiliates. All trademarks are owned by their respective owners.
