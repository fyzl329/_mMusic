package app.mmusic.android.ui.screens.home

import androidx.annotation.StringRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.mmusic.android.Database
import app.mmusic.android.LocalPlayerServiceBinder
import app.mmusic.android.R
import app.mmusic.android.models.Song
import app.mmusic.android.service.PlayerService
import app.mmusic.android.service.isLocal
import app.mmusic.android.ui.components.FadingRow
import app.mmusic.android.ui.components.LocalMenuState
import app.mmusic.android.ui.components.themed.NonQueuedMediaItemMenu
import app.mmusic.android.ui.components.themed.SecondaryTextButton
import app.mmusic.android.ui.items.SongItem
import app.mmusic.android.utils.asMediaItem
import app.mmusic.android.utils.forcePlay
import app.mmusic.android.utils.semiBold
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.providers.innertube.models.NavigationEndpoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.days
@Composable
fun HomeSectionTitle(
    text: String,
    endPaddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Dimensions.items.horizontalPadding,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val typography = LocalAppearance.current.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = 28.dp, bottom = 12.dp)
            .padding(endPaddingValues),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FadingRow(
            modifier = Modifier.weight(
                weight = 1f,
                fill = false
            )
        ) {
            BasicText(
                text = text,
                style = typography.m.semiBold
            )
        }

        if (actionText != null && onActionClick != null) SecondaryTextButton(
            text = actionText,
            onClick = onActionClick
        )
    }
}

@Composable
fun HomeRecommendations(
    currentMediaId: String?,
    isPlaying: Boolean,
    endPaddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    binder: PlayerService.Binder? = LocalPlayerServiceBinder.current
) {
    val menuState = LocalMenuState.current
    val layoutDirection = LocalLayoutDirection.current

    val recommendationWindowMs = remember { 30.days.inWholeMilliseconds }
    val now = remember { System.currentTimeMillis() }

    val recommendedSongs by remember {
        Database.trending(
            limit = 12,
            now = now,
            period = recommendationWindowMs
        )
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    val recentSongs by remember {
        Database.history(size = 12)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    val rowPadding = remember(endPaddingValues, layoutDirection) {
        PaddingValues(
            start = 16.dp,
            end = 16.dp + endPaddingValues.calculateEndPadding(layoutDirection)
        )
    }

    fun play(song: Song) {
        val mediaItem = song.asMediaItem
        binder?.stopRadio()
        binder?.player?.forcePlay(mediaItem)

        if (!song.isLocal) binder?.setupRadio(
            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
        )
    }

    Column(modifier = modifier) {
        RecommendationRow(
            titleRes = R.string.for_you,
            songs = recommendedSongs.toImmutableList(),
            rowPadding = rowPadding,
            endPaddingValues = endPaddingValues,
            currentMediaId = currentMediaId,
            isPlaying = isPlaying,
            onSongClick = ::play,
            onSongLongClick = { song ->
                menuState.display {
                    NonQueuedMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = song.asMediaItem
                    )
                }
            }
        )

        RecommendationRow(
            titleRes = R.string.keep_listening,
            songs = recentSongs.toImmutableList(),
            rowPadding = rowPadding,
            endPaddingValues = endPaddingValues,
            currentMediaId = currentMediaId,
            isPlaying = isPlaying,
            onSongClick = ::play,
            onSongLongClick = { song ->
                menuState.display {
                    NonQueuedMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = song.asMediaItem
                    )
                }
            }
        )
    }
}

@Composable
private fun RecommendationRow(
    @StringRes titleRes: Int,
    songs: ImmutableList<Song>,
    rowPadding: PaddingValues,
    endPaddingValues: PaddingValues,
    currentMediaId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    if (songs.isEmpty()) return

    Column {
        HomeSectionTitle(
            text = stringResource(id = titleRes),
            endPaddingValues = endPaddingValues
        )

        LazyRow(
            contentPadding = rowPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = songs,
                key = Song::id
            ) { song ->
                SongItem(
                    song = song,
                    thumbnailSize = Dimensions.thumbnails.song,
                    showDuration = false,
                    isPlaying = isPlaying && currentMediaId == song.id,
                    modifier = Modifier.combinedClickable(
                        onLongClick = { onSongLongClick(song) },
                        onClick = { onSongClick(song) }
                    )
                )
            }
        }
    }
}
