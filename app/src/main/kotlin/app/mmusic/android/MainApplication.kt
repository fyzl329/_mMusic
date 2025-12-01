package app.mmusic.android

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.media3.common.Player
import androidx.work.Configuration
import app.mmusic.android.BuildConfig
import app.mmusic.android.R
import app.mmusic.android.preferences.DataPreferences
import app.mmusic.android.service.PlayerService
import app.mmusic.android.service.ServiceNotifications
import app.mmusic.android.utils.LocalMonetCompat
import app.mmusic.android.utils.asMediaItem
import app.mmusic.android.utils.intent
import app.mmusic.android.utils.setDefaultPalette
import app.mmusic.android.utils.toast
import app.mmusic.compose.persist.PersistMap
import app.mmusic.compose.preferences.PreferencesHolder
import app.mmusic.core.ui.utils.isAtLeastAndroid12
import app.mmusic.core.ui.utils.songBundle
import app.mmusic.providers.innertube.Innertube
import app.mmusic.providers.innertube.models.bodies.BrowseBody
import app.mmusic.providers.innertube.requests.playlistPage
import app.mmusic.providers.innertube.requests.song
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.bitmapFactoryExifOrientationStrategy
import coil3.decode.ExifOrientationStrategy
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.kieronquinn.monetcompat.core.MonetCompat
import dev.kdrag0n.monet.theme.ColorScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    override fun onCreate() {
        val vmPolicyBuilder = VmPolicy.Builder()
            .let {
                if (isAtLeastAndroid12) it.detectUnsafeIntentLaunch() else it
            }
            .penaltyLog()

        if (BuildConfig.DEBUG) vmPolicyBuilder.penaltyDeath()

        StrictMode.setVmPolicy(vmPolicyBuilder.build())
        Dependencies.init(this)

        MonetCompat.debugLog = BuildConfig.DEBUG
        super.onCreate()

        MonetCompat.enablePaletteCompat()
        ServiceNotifications.createAll()
    }

    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.1)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("coil"))
                .maxSizeBytes(DataPreferences.coilDiskCacheMaxSize.bytes)
                .build()
        }
        .bitmapFactoryExifOrientationStrategy(ExifOrientationStrategy.IGNORE)
        .let { if (BuildConfig.DEBUG) it.logger(DebugLogger()) else it }
        .build()

    val persistMap = PersistMap()

    override val workManagerConfiguration = Configuration.Builder()
        .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
        .build()
}

object Dependencies {
    lateinit var application: MainApplication
        private set

    val py by lazy {
        if (!Python.isStarted()) Python.start(AndroidPlatform(application))
        Python.getInstance()
    }

    private val module by lazy { py.getModule("download") }

    val quickjsPath by lazy {
        File(application.applicationInfo.nativeLibraryDir, "libqjs.so")
            .also { if (!it.canExecute()) it.setExecutable(true) }
    }

    fun runDownload(id: String): String = module
        .callAttr("download", quickjsPath.absolutePath, id)
        .toString()

    fun upgradeYoutubeDl(packageName: String = "yt-dlp"): Boolean {
        val success = runCatching { module.callAttr("upgrade", packageName) }
            .also { it.exceptionOrNull()?.printStackTrace() }
            .isSuccess
        if (!success) Log.e("Python", "Upgrading $packageName resulted in non-zero exit code!")
        return success
    }

    val credentialManager by lazy { CredentialManager.create(application) }

    internal fun init(application: MainApplication) {
        this.application = application
    }
}

open class GlobalPreferencesHolder : PreferencesHolder(Dependencies.application, "preferences")
