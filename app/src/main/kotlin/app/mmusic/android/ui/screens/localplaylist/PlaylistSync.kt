package app.mmusic.android.ui.screens.localplaylist

import androidx.media3.common.MediaItem
import app.mmusic.android.Database
import app.mmusic.android.models.Playlist
import app.mmusic.android.models.SongPlaylistMap
import app.mmusic.android.transaction
import app.mmusic.android.utils.asMediaItem
import app.mmusic.android.utils.completed
import app.mmusic.providers.innertube.Innertube
import app.mmusic.providers.innertube.models.bodies.BrowseBody
import app.mmusic.providers.innertube.requests.playlistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private data class RemotePlaylistData(
    val title: String,
    val thumbnailUrl: String?,
    val mediaItems: List<MediaItem>
)

private suspend fun fetchRemotePlaylistData(
    browseId: String,
    limit: Int = 200
): RemotePlaylistData? = withContext(Dispatchers.IO) {
    val page = Innertube.playlistPage(
        BrowseBody(browseId = browseId)
    )?.completed()?.getOrNull() ?: return@withContext null

    RemotePlaylistData(
        title = page.title.orEmpty(),
        thumbnailUrl = page.thumbnail?.url,
        mediaItems = page.songsPage?.items.orEmpty()
            .map { it.asMediaItem }
            .take(limit)
    )
}

suspend fun syncPlaylistMerged(
    playlist: Playlist,
    browseId: String,
    limit: Int = 200
): Result<Unit> = runCatching {
    val remote = fetchRemotePlaylistData(browseId, limit)
        ?: error("Unable to fetch playlist")

    val existingSongs = Database.playlistSongs(playlist.id).first()
    val existingMediaItems = existingSongs.map { it.asMediaItem }

    val remoteMediaItems = remote.mediaItems
    val extraMediaItems = existingMediaItems.filter { existing ->
        remoteMediaItems.none { it.mediaId == existing.mediaId }
    }
    val finalMediaItems = remoteMediaItems + extraMediaItems

    withContext(Dispatchers.IO) {
        transaction {
            Database.clearPlaylist(playlist.id)
            finalMediaItems.forEach { Database.insert(it) }

            Database.insertSongPlaylistMaps(
                finalMediaItems.mapIndexed { index, mediaItem ->
                    SongPlaylistMap(
                        songId = mediaItem.mediaId,
                        playlistId = playlist.id,
                        position = index
                    )
                }
            )

            Database.update(
                playlist.copy(
                    name = remote.title.ifBlank { playlist.name },
                    browseId = browseId,
                    thumbnail = playlist.thumbnail ?: remote.thumbnailUrl
                )
            )
        }
    }
}
