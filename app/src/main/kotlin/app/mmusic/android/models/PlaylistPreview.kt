package app.mmusic.android.models

import androidx.compose.runtime.Immutable

@Immutable
data class PlaylistPreview(
    val id: Long,
    val name: String,
    val songCount: Int,
    val thumbnail: String?,
    val browseId: String?
) {
    val playlist by lazy {
        Playlist(
            id = id,
            name = name,
            browseId = browseId
        )
    }
}
