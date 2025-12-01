# Architecture

## Layers and modules
- App (`app/`): Android entry, Compose UI, navigation, playback service (ExoPlayer), Room schemas, Play assets.
- Core (`core/`): Shared models (`data`), theming bridge (`material-compat`), UI components (`ui`).
- Compose utils (`compose/`): Persistence, preferences, routing, and reordering helpers reused across features.
- Providers (`providers/`): Service clients on Ktor (Innertube, Piped, Kugou, LrcLib, GitHub, translate, SponsorBlock). `providers/common` hosts shared contracts.
- Networking (`ktor-client-brotli/`): Brotli-enabled client for providers.
- Release (`fastlane/`): Metadata and screenshots; update when UX changes.

## Data and UI flow
1) Compose screens in `app` render from view-model/state holders.
2) Data sources come from local DB (Room) and remote providers. Providers wrap Ktor clients with typed models and handle response parsing.
3) UI events -> view models -> repositories/providers -> results flow back to state. State drives composables.
4) Preferences (dynamic theme, playback options) use the `compose/preferences` helper and are injected into screens.

## Playback pipeline
- Player: ExoPlayer with a service/binder in `app`. Media items are built from local songs or provider items.
- Play-first streaming: For remote songs we start playback immediately while streaming; if an unknown/stuck state occurs, we fall back to fetch-first and show a fetching animation so the user sees progress.
- Radio/recommendations: When playing a non-local track we start radio using Innertube endpoints; related songs update the “For You” rail and influence “Popular” playlists.
- Error handling: Unknown errors trigger the fallback path; local songs bypass radio setup.

## Recommendations
- Trending (local DB) seeds related requests.
- Related page (Innertube) provides “For You” songs and “Popular” playlists. The lists refresh after each song change to reflect the latest genre/context.

## Theming and layout
- Compose-driven UI with shared dimensions and color tokens in `core/ui` and `core/material-compat`.
- Insets and player-aware paddings are applied in Home/Library to keep headers pinned and avoid overlap with the mini-player.

## Testing and quality
- Static analysis: `./gradlew detekt lint`
- Unit tests (where present): `./gradlew :app:testDebugUnitTest`
- Instrumentation: `./gradlew :app:connectedDebugAndroidTest`
- Keep Room schemas in `app/schemas` updated when entities change to validate migrations.

## Configuration and security
- Do not commit secrets. Signing uses `ANDROID_NIGHTLY_*` environment variables.
- `local.properties` should only contain SDK/NDK paths.
