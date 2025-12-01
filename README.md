
# mMusic
A friendly music app for Android. It plays your YouTube Music favorites, mixes in your local library, and keeps tracks handy offline. Built to feel light, fast, and personal.

## Features
- Stream most YouTube Music songs, albums, and playlists (watch/playlist/channel links work).
- Local library playback with background play and optional loudness normalization.
- Offline caching so your favorites are available on the go.
- Search across songs, albums, artists, videos, and playlists; browse moods and genres.
- View and edit synced lyrics.
- Import/export playlists (YouTube) plus local playlist management.
- Android Auto support and flexible theming (Material You).

## Tech used
- Jetpack Compose for UI
- Kotlin coroutines/Flow
- Ktor for network providers (Innertube, Piped, Kugou, LrcLib, GitHub, translate, SponsorBlock)
- Room for local data and schemas
- ExoPlayer for playback
- Fastlane for release metadata

## Project layout
- `app/` – Android app (Compose UI, playback service, Room schemas, Play assets).
- `core/` – Shared UI, theming bridge, and data models.
- `compose/` – Small Compose helpers (persist, preferences, routing, reordering).
- `providers/` – Service integrations built on Ktor.
- `fastlane/` – Release metadata and screenshots.

More detail lives in `docs/ARCHITECTURE.md`.

## Acknowledgments
- Heavily inspired by **Vitune** by Bart Ostooven.
- [YouTube-Internal-Clients](https://github.com/zerodytrash/YouTube-Internal-Clients)

## Disclaimer
This project is not affiliated with YouTube, Google LLC, or their affiliates. All trademarks are owned by their respective owners.
