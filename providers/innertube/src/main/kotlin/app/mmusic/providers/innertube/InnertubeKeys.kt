package app.mmusic.providers.innertube

private const val DEFAULT_KEY = "INNERTUBE_API_KEY"
private const val WEB_KEY = "INNERTUBE_API_KEY_WEB"
private const val IOS_KEY = "INNERTUBE_API_KEY_IOS"
private const val ANDROID_KEY = "INNERTUBE_API_KEY_ANDROID"
private const val ANDROID_MUSIC_KEY = "INNERTUBE_API_KEY_ANDROID_MUSIC"

/**
 * Central place to source Innertube API keys without committing them.
 *
 * The preferred flow is:
 * 1. The Android app sets keys at startup via [configure] using BuildConfig values.
 * 2. For other runtimes (tests/desktop), env or JVM properties with the names above are read.
 */
object InnertubeKeys {
    private data class ProvidedKeys(
        val web: String,
        val ios: String,
        val android: String,
        val androidMusic: String
    )

    @Volatile
    private var provided: ProvidedKeys? = null

    fun configure(
        web: String,
        ios: String = web,
        android: String = web,
        androidMusic: String = android
    ) {
        provided = ProvidedKeys(
            web = web.trim(),
            ios = ios.trim(),
            android = android.trim(),
            androidMusic = androidMusic.trim()
        )
    }

    val webApiKey: String
        get() = requireKey(WEB_KEY)

    val iosApiKey: String
        get() = requireKey(IOS_KEY, WEB_KEY)

    val androidApiKey: String
        get() = requireKey(ANDROID_KEY, WEB_KEY)

    val androidMusicApiKey: String
        get() = requireKey(ANDROID_MUSIC_KEY, ANDROID_KEY, WEB_KEY)

    private fun requireKey(primary: String, vararg fallbacks: String): String =
        resolve(primary, *fallbacks)
            ?: error(
                "Missing Innertube API key. Set $primary " +
                    fallbacks.takeIf { it.isNotEmpty() }?.joinToString(
                        prefix = "(or ",
                        postfix = ")"
                    ).orEmpty() +
                    " via env or Gradle property."
            )

    private fun resolve(primary: String, vararg fallbacks: String): String? {
        val providedValue = provided?.valueFor(primary)?.takeIf { it.isNotBlank() }
        if (providedValue != null) return providedValue

        return readKey(primary, *fallbacks, DEFAULT_KEY)
    }

    private fun readKey(vararg names: String): String? =
        names.asSequence()
            .mapNotNull { name ->
                System.getProperty(name)?.takeIf { it.isNotBlank() }
                    ?: System.getenv(name)?.takeIf { it.isNotBlank() }
            }
            .firstOrNull()

    private fun ProvidedKeys.valueFor(name: String): String? =
        when (name) {
            WEB_KEY, DEFAULT_KEY -> web
            IOS_KEY -> ios
            ANDROID_KEY -> android
            ANDROID_MUSIC_KEY -> androidMusic
            else -> null
        }
}
