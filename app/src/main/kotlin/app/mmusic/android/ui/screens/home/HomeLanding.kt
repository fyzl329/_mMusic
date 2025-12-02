package app.mmusic.android.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.mmusic.core.ui.utils.isLandscape
import app.mmusic.android.Database
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.LocalPlayerServiceBinder
import app.mmusic.android.R
import app.mmusic.android.models.Song
import app.mmusic.android.preferences.AppearancePreferences
import app.mmusic.android.preferences.DataPreferences
import app.mmusic.android.service.LOCAL_KEY_PREFIX
import app.mmusic.android.service.isLocal
import app.mmusic.android.ui.components.LocalMenuState
import app.mmusic.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.mmusic.android.ui.components.themed.NonQueuedMediaItemMenu
import app.mmusic.android.ui.components.themed.SecondaryTextButton
import app.mmusic.android.ui.components.themed.PrimaryButton
import app.mmusic.android.ui.screens.playlistRoute
import app.mmusic.android.utils.asMediaItem
import app.mmusic.android.utils.forcePlay
import app.mmusic.android.utils.playingSong
import app.mmusic.android.utils.secondary
import app.mmusic.android.utils.semiBold
import app.mmusic.android.utils.thumbnail
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.core.ui.overlay
import app.mmusic.providers.innertube.Innertube
import app.mmusic.providers.innertube.models.NavigationEndpoint
import app.mmusic.providers.innertube.models.bodies.NextBody
import app.mmusic.providers.innertube.requests.relatedPage
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeLanding(
    onBuiltInPlaylist: (app.mmusic.core.data.enums.BuiltInPlaylist) -> Unit,
    onPlaylistClick: (app.mmusic.android.models.Playlist) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography, _, cornerShape) = LocalAppearance.current
    val lazyListState = rememberLazyListState()
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val (currentMediaId, playing) = playingSong(binder)
    val isFirstRun = remember { !DataPreferences.hasSeenHomeTutorial }
    var showWelcome by remember { mutableStateOf(isFirstRun) }
    var showTutorial by remember { mutableStateOf(isFirstRun) }

    LaunchedEffect(showTutorial) {
        if (showTutorial && !DataPreferences.hasSeenHomeTutorial) {
            DataPreferences.hasSeenHomeTutorial = true
        }
    }

    val trendingSongs by remember {
        Database.trending(limit = 12)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    val historySongs by remember {
        Database.history(size = 50)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    var relatedPage by remember { mutableStateOf<Innertube.RelatedPage?>(null) }
    var lastRelatedSeedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trendingSongs.firstOrNull(), historySongs) {
        val seed = historySongs.firstOrNull { !it.isLocal } ?: trendingSongs.firstOrNull()
        val seedId = seed?.id ?: return@LaunchedEffect
        if (seedId == lastRelatedSeedId) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            relatedPage = Innertube.relatedPage(body = NextBody(videoId = seedId))
                ?.getOrNull()
            lastRelatedSeedId = seedId
        }
    }

    LaunchedEffect(currentMediaId) {
        val mediaId = currentMediaId ?: return@LaunchedEffect
        if (mediaId.startsWith(LOCAL_KEY_PREFIX)) {
            relatedPage = null
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            relatedPage = Innertube.relatedPage(body = NextBody(videoId = mediaId))
                ?.getOrNull()
            lastRelatedSeedId = mediaId
        }
    }

    fun playLocalSong(song: Song) {
        val mediaItem = song.asMediaItem
        binder?.stopRadio()
        binder?.player?.forcePlay(mediaItem)

        if (!song.isLocal) binder?.setupRadio(
            NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
        )
    }

    fun playInnertubeSong(song: Innertube.SongItem) {
        val mediaItem = song.asMediaItem
        binder?.stopRadio()
        binder?.player?.forcePlay(mediaItem)
        binder?.setupRadio(song.info?.endpoint)
    }

    val insetsPadding = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
        .asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorPalette.background0)
    ) {
        val bottomContentPadding = with(LocalDensity.current) {
            val playerInsetDp = LocalPlayerAwareWindowInsets.current.getBottom(this).toDp()
            val navigationBarHeightDp = if (isLandscape) 0.dp else 84.dp
            playerInsetDp + navigationBarHeightDp + Dimensions.items.verticalPadding
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insetsPadding)
                .padding(horizontal = Dimensions.items.horizontalPadding)
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (showWelcome) {
                    item("hero") {
                        HomeHero(
                            onSearchClick = onSearchClick,
                            showTutorial = showTutorial,
                            onDismissTutorial = {
                                showTutorial = false
                                showWelcome = false
                                DataPreferences.hasSeenHomeTutorial = true
                            }
                        )
                    }
                } else {
                    item("header") {
                        HomeHeader(
                            onSearchClick = onSearchClick
                        )
                    }
                }

                item("for_you") {
                    SectionContainer {
                        val relatedSongs = relatedPage?.songs
                        val contentSongs = relatedSongs?.takeIf { it.isNotEmpty() }
                            ?: trendingSongs.toImmutableList()
                        val hasSongs = contentSongs.isNotEmpty()

                        SectionHeader(
                            title = stringResource(R.string.for_you),
                            description = stringResource(
                                if (relatedSongs.isNullOrEmpty()) {
                                    R.string.home_recommendation_basis_trending
                                } else {
                                    R.string.home_recommendation_basis_recent
                                }
                            )
                        )

                        if (hasSongs) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.items.horizontalPadding),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                items(contentSongs, key = {
                                    when (it) {
                                        is Innertube.SongItem -> it.key
                                        is Song -> it.id
                                        else -> it.hashCode()
                                    }
                                }) { song ->
                                    when (song) {
                                        is Innertube.SongItem -> ForYouSongCard(
                                            title = song.info?.name.orEmpty(),
                                            subtitle = song.authors?.joinToString("") { it.name.orEmpty() }
                                                ?: "",
                                            thumbnailUrl = song.thumbnail?.url,
                                            isPlaying = playing && currentMediaId == song.key,
                                            onClick = { playInnertubeSong(song) },
                                            onLongClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        onDismiss = menuState::hide,
                                                        mediaItem = song.asMediaItem
                                                    )
                                                }
                                            }
                                        )

                                        is Song -> ForYouSongCard(
                                            title = song.title,
                                            subtitle = song.artistsText.orEmpty(),
                                            thumbnailUrl = song.thumbnailUrl,
                                            isPlaying = playing && currentMediaId == song.id,
                                            onClick = { playLocalSong(song) },
                                            onLongClick = {
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
                            }
                        } else {
                            BasicText(
                                text = stringResource(R.string.home_empty_message),
                                style = typography.s.secondary.copy(color = colorPalette.text)
                            )
                        }
                    }
                }

                item("history") {
                    if (historySongs.isNotEmpty()) {
                        SectionContainer {
                            SectionHeader(
                                title = stringResource(R.string.recently_played),
                                actionText = stringResource(R.string.home_discover_music),
                                onActionClick = onSearchClick
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.items.horizontalPadding),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                items(historySongs.take(12), key = { it.id }) { song ->
                                    AlbumCard(
                                        song = song,
                                        modifier = Modifier.width(160.dp),
                                        onClick = { playLocalSong(song) },
                                        onLongClick = {
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
                        }
                    }
                }

                item("popular_row") {
                    val playlists = relatedPage?.playlists.orEmpty()
                    SectionContainer {
                        SectionHeader(
                            title = stringResource(R.string.home_popular_playlists),
                            actionText = stringResource(R.string.view_all),
                            onActionClick = onSearchClick
                        )

                        if (playlists.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.items.horizontalPadding),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(playlists, key = { it.key }) { playlist ->
                                    PlaylistCard(
                                        card = playlist,
                                        modifier = Modifier
                                            .width(200.dp)
                                            .clickable {
                                                playlistRoute.global(
                                                    playlist.key,
                                                    playlist.info?.endpoint?.params,
                                                    null,
                                                    false
                                                )
                                            }
                                    )
                                }
                            }
                        } else {
                            BasicText(
                                text = stringResource(R.string.home_empty_message),
                                style = typography.s.secondary.copy(color = colorPalette.text)
                            )
                        }
                    }
                }
            }
        }

        FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
    }
}

@Composable
private fun HomeHero(
    onSearchClick: () -> Unit,
    showTutorial: Boolean,
    onDismissTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography, _, cornerShape) = LocalAppearance.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, cornerShape)
            .clip(cornerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorPalette.background1,
                        colorPalette.background0
                    )
                )
            )
            .padding(horizontal = Dimensions.items.horizontalPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    text = stringResource(R.string.home_intro_title),
                    style = typography.xxl.semiBold.copy(color = colorPalette.text)
                )
                BasicText(
                    text = stringResource(R.string.home_hero_subtitle),
                    style = typography.m.secondary.copy(color = colorPalette.text)
                )
            }

            PrimaryButton(
                onClick = onSearchClick,
                icon = R.drawable.search
            )
        }

        if (showTutorial) {
            TutorialTips(onDismiss = onDismissTutorial)
        } else {
            SecondaryTextButton(
                text = stringResource(R.string.home_hero_action_primary),
                onClick = onSearchClick,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun TutorialTips(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicText(
            text = stringResource(R.string.home_tutorial_title),
            style = typography.l.semiBold.copy(color = colorPalette.text)
        )

        TipRow(text = stringResource(R.string.home_tutorial_step_search))
        TipRow(text = stringResource(R.string.home_tutorial_step_long_press))
        TipRow(text = stringResource(R.string.home_tutorial_step_quick_picks))

        SecondaryTextButton(
            text = stringResource(R.string.home_tutorial_action),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun HomeHeader(
    onSearchClick: () -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.items.verticalPadding)
    ) {
        BasicText(
            text = stringResource(R.string.home_title),
            style = typography.xxl.semiBold.copy(color = colorPalette.text)
        )

        SecondaryTextButton(
            text = stringResource(R.string.home_hero_action_primary),
            onClick = onSearchClick
        )
    }
}

@Composable
private fun TipRow(text: String) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorPalette.text)
        )
        BasicText(
            text = text,
            style = typography.s.secondary.copy(color = colorPalette.text)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = title,
                style = typography.l.semiBold.copy(color = colorPalette.text)
            )

            if (actionText != null && onActionClick != null) {
                SecondaryTextButton(text = actionText, onClick = onActionClick)
            }
        }

        if (!description.isNullOrBlank()) {
            BasicText(
                text = description,
                style = typography.s.secondary.copy(color = colorPalette.text)
            )
        }
    }
}

@Composable
private fun SectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette, _, _, cornerShape) = LocalAppearance.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, cornerShape)
            .clip(cornerShape)
            .background(colorPalette.background1)
            .padding(horizontal = Dimensions.items.horizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}

@Composable
private fun AlbumCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val (colorPalette, typography, _, cornerShape) = LocalAppearance.current
    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = song.thumbnailUrl?.thumbnail(AppearancePreferences.maxThumbnailSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cornerShape)
        )
        BasicText(
            text = song.title,
            style = typography.m.semiBold.copy(color = colorPalette.text)
        )
        BasicText(
            text = song.artistsText.orEmpty(),
            style = typography.xs.secondary.copy(color = colorPalette.text.copy(alpha = 0.75f)),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun PlaylistCard(
    card: Innertube.PlaylistItem,
    modifier: Modifier = Modifier
) {
    val (colorPalette, _, _, cornerShape) = LocalAppearance.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                card.thumbnail?.url?.thumbnail(AppearancePreferences.maxThumbnailSize)
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cornerShape)
        )
    }
}

@Composable
private fun ForYouSongCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (colorPalette, typography, _, cornerShape) = LocalAppearance.current
    val tileWidth = Dimensions.thumbnails.album + 32.dp

    Column(
        modifier = Modifier
            .width(tileWidth)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            AsyncImage(
                model = thumbnailUrl?.thumbnail(AppearancePreferences.maxThumbnailSize),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cornerShape)
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorPalette.overlay.copy(alpha = 0.1f), shape = cornerShape)
                )
            }
        }

        BasicText(
            text = title,
            style = typography.m.semiBold.copy(color = colorPalette.text),
            maxLines = 2
        )
        BasicText(
            text = subtitle,
            style = typography.s.secondary.copy(color = colorPalette.text),
            maxLines = 1
        )
    }
}
