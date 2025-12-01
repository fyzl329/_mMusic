package app.mmusic.android.ui.screens.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.LocalPlayerServiceBinder
import app.mmusic.android.R
import app.mmusic.android.models.Song
import app.mmusic.android.ui.components.LocalMenuState
import app.mmusic.android.ui.components.ShimmerHost
import app.mmusic.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.mmusic.android.ui.components.themed.LayoutWithAdaptiveThumbnail
import app.mmusic.android.ui.components.themed.NonQueuedMediaItemMenu
import app.mmusic.android.ui.components.themed.SecondaryTextButton
import app.mmusic.android.ui.items.SongItem
import app.mmusic.android.ui.items.SongItemPlaceholder
import app.mmusic.android.utils.PlaylistDownloadIcon
import app.mmusic.android.utils.asMediaItem
import app.mmusic.android.utils.asNavigationBarAwarePaddingValues
import app.mmusic.android.utils.enqueue
import app.mmusic.android.utils.forcePlayAtIndex
import app.mmusic.android.utils.forcePlayFromBeginning
import app.mmusic.android.utils.playingSong
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.core.ui.utils.isLandscape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
// TODO: migrate to single impl for all 'song lists'
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumSongs(
    songs: ImmutableList<Song>,
    headerContent: @Composable (
        beforeContent: (@Composable () -> Unit)?,
        afterContent: (@Composable () -> Unit)?
    ) -> Unit,
    thumbnailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    afterHeaderContent: (@Composable () -> Unit)? = null
) = LayoutWithAdaptiveThumbnail(
    thumbnailContent = thumbnailContent,
    modifier = modifier
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val lazyListState = rememberLazyListState()

    val (currentMediaId, playing) = playingSong(binder)

    Box {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asNavigationBarAwarePaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
                .padding(
                    horizontal = Dimensions.items.horizontalPadding,
                    vertical = Dimensions.items.verticalPadding / 2
                )
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    headerContent(
                        {
                            SecondaryTextButton(
                                text = stringResource(R.string.enqueue),
                                enabled = songs.isNotEmpty(),
                                onClick = {
                                    binder?.player?.enqueue(songs.map(Song::asMediaItem))
                                }
                            )
                        },
                        {
                            PlaylistDownloadIcon(
                                songs = songs.map(Song::asMediaItem).toImmutableList()
                            )
                        }
                    )

                    if (!isLandscape) thumbnailContent()
                    afterHeaderContent?.invoke()
                }
            }

            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                SongItem(
                    song = song,
                    index = index,
                    thumbnailSize = Dimensions.thumbnails.song,
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem
                                )
                            }
                        },
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(
                                items = songs.map(Song::asMediaItem),
                                index = index
                            )
                        }
                    ),
                    isPlaying = playing && currentMediaId == song.id
                )
            }

            if (songs.isEmpty()) item(key = "loading") {
                ShimmerHost(modifier = Modifier.fillParentMaxSize()) {
                    repeat(4) {
                        SongItemPlaceholder(thumbnailSize = Dimensions.thumbnails.song)
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                if (songs.isNotEmpty()) {
                    binder?.stopRadio()
                    binder?.player?.forcePlayFromBeginning(
                        songs.shuffled().map(Song::asMediaItem)
                    )
                }
            }
        )
    }
}
