package app.mmusic.android.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import app.mmusic.core.ui.utils.isLandscape
import app.mmusic.android.Database
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.R
import app.mmusic.android.models.Playlist
import app.mmusic.android.preferences.DataPreferences
import app.mmusic.android.preferences.OrderPreferences
import app.mmusic.android.query
import app.mmusic.android.ui.components.themed.DefaultDialog
import app.mmusic.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.mmusic.android.ui.components.themed.SecondaryTextButton
import app.mmusic.android.ui.components.themed.TextFieldDialog
import app.mmusic.android.ui.items.PlaylistItem
import app.mmusic.android.ui.screens.Route
import app.mmusic.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.mmusic.android.ui.screens.localplaylist.syncPlaylistMerged
import app.mmusic.android.utils.color
import app.mmusic.android.utils.semiBold
import app.mmusic.android.utils.toast
import app.mmusic.core.data.enums.BuiltInPlaylist
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(UnstableApi::class)
@Route
@Composable
fun HomeLibrary(
    onBuiltInPlaylist: (BuiltInPlaylist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) = with(OrderPreferences) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val insetsPadding = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
        .asPaddingValues()

    val lazyGridState = rememberLazyGridState()
    val builtInPlaylists by BuiltInPlaylistScreen.shownPlaylistsAsState()

    val playlistFlow = remember(playlistSortBy, playlistSortOrder) {
        app.mmusic.android.Database.playlistPreviews(playlistSortBy, playlistSortOrder)
    }
    val playlistItems by playlistFlow.collectAsState(
        initial = emptyList(),
        context = Dispatchers.IO
    )

    val pinned = remember(builtInPlaylists) {
        buildList {
            if (BuiltInPlaylist.Offline in builtInPlaylists) add(
                PinnedEntry(BuiltInPlaylist.Offline, R.drawable.airplane)
            )
            if (BuiltInPlaylist.Favorites in builtInPlaylists) add(
                PinnedEntry(BuiltInPlaylist.Favorites, R.drawable.heart)
            )
            if (BuiltInPlaylist.History in builtInPlaylists) add(
                PinnedEntry(BuiltInPlaylist.History, R.drawable.history)
            )
            if (BuiltInPlaylist.Top in builtInPlaylists) add(
                PinnedEntry(BuiltInPlaylist.Top, R.drawable.trending)
            )
        }
    }

    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var showAddOptions by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importUrl by rememberSaveable { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }

    suspend fun importPlaylistFromUrl(url: String, limit: Int = 200) {
        val listId = extractPlaylistId(url) ?: run {
            withContext(Dispatchers.Main) {
                context.toast(R.string.invalid_playlist_url)
            }
            return
        }

        val browseId = if (listId.startsWith("VL")) listId else "VL$listId"

        withContext(Dispatchers.IO) {
            val existing = Database.playlistByBrowseId(browseId)
            val playlistId = existing?.id ?: Database.insert(
                Playlist(
                    name = "YouTube playlist",
                    browseId = browseId
                )
            )

            syncPlaylistMerged(
                playlist = (existing ?: Playlist(id = playlistId, name = "YouTube playlist", browseId = browseId)),
                browseId = browseId,
                limit = limit
            ).getOrThrow()
        }

        withContext(Dispatchers.Main) {
            context.toast(android.R.string.ok)
        }
    }

    if (isCreatingANewPlaylist) TextFieldDialog(
        hintText = stringResource(R.string.enter_playlist_name_prompt),
        onDismiss = { isCreatingANewPlaylist = false },
        onAccept = { text ->
            if (text.isNotBlank()) query { Database.insert(Playlist(name = text.trim())) }
            isCreatingANewPlaylist = false
        }
    )

    val bottomPadding = with(LocalDensity.current) {
        val playerInsetDp = LocalPlayerAwareWindowInsets.current.getBottom(this).toDp()
        val navigationBarHeightDp = if (isLandscape) 0.dp else 84.dp
        playerInsetDp + navigationBarHeightDp + Dimensions.items.verticalPadding
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetsPadding)
                .padding(horizontal = Dimensions.items.horizontalPadding)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.items.verticalPadding)
            ) {
                BasicText(
                    text = stringResource(R.string.library),
                    style = typography.xxl.semiBold.color(colorPalette.text)
                )

                SecondaryTextButton(
                    text = stringResource(R.string.add_playlist),
                    onClick = { showAddOptions = true },
                    enabled = !importing
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = lazyGridState,
                userScrollEnabled = true,
                verticalArrangement = Arrangement.spacedBy(Dimensions.items.verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.items.horizontalPadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(
                    bottom = bottomPadding
                )
            ) {
                items(items = pinned, key = { it.playlist.name }) { entry ->
                    val title = when (entry.playlist) {
                        BuiltInPlaylist.Favorites -> stringResource(R.string.favorites)
                        BuiltInPlaylist.Offline -> stringResource(R.string.offline)
                        BuiltInPlaylist.History -> stringResource(R.string.history)
                        BuiltInPlaylist.Top -> stringResource(
                            R.string.format_my_top_playlist,
                            DataPreferences.topListLength
                        )
                    }
                    PlaylistItem(
                        icon = entry.icon,
                        colorTint = colorPalette.text,
                        name = title,
                        songCount = null,
                        thumbnailSize = Dimensions.thumbnails.playlist,
                        alternative = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBuiltInPlaylist(entry.playlist) }
                    )
                }

                items(
                    items = playlistItems,
                    key = { it.id }
                ) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        thumbnailSize = Dimensions.thumbnails.playlist,
                        isSynced = playlist.browseId != null,
                        alternative = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPlaylistClick(
                                    Playlist(
                                        id = playlist.id,
                                        name = playlist.name
                                    )
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.items.verticalPadding))
        }

        FloatingActionsContainerWithScrollToTop(lazyGridState = lazyGridState)
    }

    if (showAddOptions) {
        DefaultDialog(
            onDismiss = { showAddOptions = false }
        ) {
            SecondaryTextButton(
                text = stringResource(R.string.import_youtube_playlist),
                onClick = {
                    showAddOptions = false
                    showImportDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            SecondaryTextButton(
                text = stringResource(R.string.new_playlist),
                onClick = {
                    showAddOptions = false
                    isCreatingANewPlaylist = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }

    if (showImportDialog) {
        TextFieldDialog(
            hintText = stringResource(R.string.paste_playlist_url),
            onDismiss = { showImportDialog = false },
            onAccept = { url ->
                showImportDialog = false
                importUrl = url
                coroutineScope.launch {
                    importing = true
                    runCatching { importPlaylistFromUrl(url) }
                        .onFailure { context.toast(R.string.invalid_playlist_url) }
                    importing = false
                }
            }
        )
    }
}

private data class PinnedEntry(
    val playlist: BuiltInPlaylist,
    val icon: Int
)

private fun extractPlaylistId(input: String): String? = runCatching {
    val uri = Uri.parse(input.trim())
    uri.getQueryParameter("list") ?: uri.lastPathSegment
}.getOrNull()?.takeIf { it.isNotBlank() }
