gem# mMusic Project Overview

mMusic is an Android application designed for seamless music streaming. It offers a rich set of features including playing music and videos from YouTube Music, local device playback, background audio, offline caching, and playlist management. The application is built with a strong focus on user experience, offering a highly customizable interface with dynamic themes and Material You support, and integration with Android Auto. A unique aspect of its architecture is the integration of Python scripts, specifically `yt-dlp`, via the Chaquopy plugin for fetching media content.

## Technologies Used

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose
*   **Build System:** Gradle (Kotlin DSL)
*   **Static Analysis:** Detekt
*   **Database:** Room Persistence Library
*   **Media Playback:** ExoPlayer, Media3
*   **Image Loading:** Coil
*   **Asynchronous Programming:** Kotlin Coroutines
*   **Python Integration:** Chaquopy (for `yt-dlp` and `yt-dlp-ejs`)
*   **Native Development:** CMake (for C++ components)
*   **Other Libraries:** Kotlinx Serialization, Kotlinx Immutable, Kotlinx Datetime, Log4j, SLF4J, Logback.

## Building and Running

This project is an Android application and can be built and run using Android Studio.

To build the project from the command line:

```bash
./gradlew assembleDebug
```

To run static analysis checks:

```bash
./gradlew detekt
```

## Development Conventions

*   **Static Analysis:** The project utilizes Detekt for static code analysis to maintain code quality and consistency. The configuration for Detekt is located in `detekt.yml`.
*   **Kotlin Version:** The project targets Kotlin 2.2.
*   **Compose Compiler:** Compose compiler reports can be enabled by setting the `enableComposeCompilerReports` project property to `true`.
