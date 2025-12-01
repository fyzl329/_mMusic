package app.mmusic.android.models.ui

import androidx.media3.common.MediaItem
import app.mmusic.core.ui.utils.songBundle

data class UiMedia(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val explicit: Boolean
)

fun MediaItem.toUiMedia(duration: Long) = UiMedia(
    id = mediaId,
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
    duration = duration,
    explicit = mediaMetadata.extras?.songBundle?.explicit == true
)
