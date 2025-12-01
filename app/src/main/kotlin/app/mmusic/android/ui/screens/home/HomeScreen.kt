package app.mmusic.android.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.mmusic.android.Database
import app.mmusic.android.LocalPlayerServiceBinder
import app.mmusic.android.R
import app.mmusic.android.handleUrl
import app.mmusic.android.models.SearchQuery
import app.mmusic.android.preferences.DataPreferences
import app.mmusic.android.preferences.UIStatePreferences
import app.mmusic.android.query
import app.mmusic.android.ui.components.themed.Scaffold
import app.mmusic.android.ui.screens.GlobalRoutes
import app.mmusic.android.ui.screens.Route
import app.mmusic.android.ui.screens.builtInPlaylistRoute
import app.mmusic.android.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import app.mmusic.android.ui.screens.localPlaylistRoute
import app.mmusic.android.ui.screens.localplaylist.LocalPlaylistScreen
import app.mmusic.android.ui.screens.search.SearchContent
import app.mmusic.android.ui.screens.searchResultRoute
import app.mmusic.android.ui.screens.searchRoute
import app.mmusic.android.ui.screens.settings.SettingsContent
import app.mmusic.android.utils.toast
import app.mmusic.core.ui.LocalAppearance
import kotlinx.coroutines.launch
import app.mmusic.compose.persist.PersistMapCleanup
import app.mmusic.compose.routing.RouteHandler
import app.mmusic.android.LocalPlayerAwareWindowInsets
@Route
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val saveableStateHolder = rememberSaveableStateHolder()
    val pagerScope = rememberCoroutineScope()
    val initialPage = 1

    PersistMapCleanup("home/")

    RouteHandler {
        GlobalRoutes()

        localPlaylistRoute { playlistId ->
            LocalPlaylistScreen(playlistId = playlistId)
        }

        builtInPlaylistRoute { builtInPlaylist ->
            BuiltInPlaylistScreen(builtInPlaylist = builtInPlaylist)
        }

        Content {
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { 4 }
            )

            LaunchedEffect(pagerState.currentPage) {
                UIStatePreferences.homeScreenTabIndex = pagerState.currentPage.coerceIn(0, 3)
            }

            Scaffold(
                key = "home",
                topIconButtonId = R.drawable.settings,
                onTopIconButtonClick = {
                    pagerScope.launch { pagerState.animateScrollToPage(0) }
                },
                tabIndex = pagerState.currentPage,
                onTabChange = { page ->
                    pagerScope.launch {
                        pagerState.animateScrollToPage(page.coerceIn(0, 3))
                    }
                },
                tabColumnContent = { },
                modifier = modifier,
                showNavigationBar = false
            ) { _ ->
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(state = pagerState) { page ->
                        val onSearchClick: () -> Unit = {
                            searchRoute.global("")
                            Unit
                        }

                        saveableStateHolder.SaveableStateProvider(key = page) {
                            when (page) {
                                0 -> SettingsContent(
                                    topIconButtonId = 0,
                                    onTopIconButtonClick = { pagerScope.launch { pagerState.animateScrollToPage(1) } },
                                    showNavigationBar = true
                                )

                                1 -> HomeLanding(
                                    onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                                    onPlaylistClick = { localPlaylistRoute(it.id) },
                                    onSearchClick = onSearchClick
                                )

                                2 -> HomeLibrary(
                                    onBuiltInPlaylist = { builtInPlaylistRoute(it) },
                                    onPlaylistClick = { localPlaylistRoute(it.id) }
                                )

                                3 -> SearchContent(
                                    initialTextInput = "",
                                    onSearch = { query ->
                                        searchResultRoute.global(query)
                                        if (!DataPreferences.pauseSearchHistory) {
                                            query {
                                                Database.insert(SearchQuery(query = query))
                                            }
                                        }
                                    },
                                    onViewPlaylist = { url ->
                                        with(context) {
                                            runCatching {
                                                handleUrl(url.toUri(), binder)
                                            }.onFailure {
                                                toast(getString(R.string.error_url, url))
                                            }
                                        }
                                    },
                                    topIconButtonId = 0,
                                    onTopIconButtonClick = { },
                                    showNavigationBar = true,
                                    shouldAutoFocus = false
                                )

                                else -> { }
                            }
                        }
                    }

            PageIndicators(
                currentPage = pagerState.currentPage,
                onSelect = { target ->
                    pagerScope.launch {
                        pagerState.animateScrollToPage(target.coerceIn(0, 3))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = with(LocalDensity.current) {
                            LocalPlayerAwareWindowInsets.current.getBottom(this).toDp() + 12.dp
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PageIndicators(
    currentPage: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = modifier
            .background(
                colorPalette.background1.copy(alpha = 0.9f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val entries = listOf(
                R.drawable.settings,
                R.drawable.musical_notes,
                R.drawable.library,
                R.drawable.search
            )
            entries.forEachIndexed { index, icon ->
                val selected = index == currentPage
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (selected) colorPalette.accent else colorPalette.textSecondary
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onSelect(index) }
                        .padding(6.dp)
                )
            }
        }
    }
}
