package app.mmusic.android.ui.screens.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.mmusic.android.Database
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.R
import app.mmusic.android.models.SearchQuery
import app.mmusic.android.preferences.DataPreferences
import app.mmusic.android.query
import app.mmusic.android.ui.components.themed.FloatingActionsContainerWithScrollToTop
import app.mmusic.android.ui.components.themed.Header
import app.mmusic.android.ui.components.themed.SecondaryTextButton
import app.mmusic.android.utils.align
import app.mmusic.android.utils.center
import app.mmusic.android.utils.disabled
import app.mmusic.android.utils.medium
import app.mmusic.android.utils.secondary
import app.mmusic.compose.persist.persist
import app.mmusic.compose.persist.persistList
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.providers.innertube.Innertube
import app.mmusic.providers.innertube.models.bodies.SearchSuggestionsBody
import app.mmusic.providers.innertube.requests.searchSuggestions
import io.ktor.http.Url
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OnlineSearch(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit,
    focused: Boolean,
    modifier: Modifier = Modifier
) = Box(modifier = modifier) {
    val (colorPalette, typography) = LocalAppearance.current

    var history by persistList<SearchQuery>("search/online/history")
    var suggestionsResult by persist<Result<List<String>?>?>("search/online/suggestionsResult")

    LaunchedEffect(textFieldValue.text) {
        if (DataPreferences.pauseSearchHistory) return@LaunchedEffect

        Database.queries("%${textFieldValue.text}%")
            .distinctUntilChanged { old, new -> old.size == new.size }
            .collect { history = it.toImmutableList() }
    }

    LaunchedEffect(textFieldValue.text) {
        if (textFieldValue.text.isEmpty()) return@LaunchedEffect

        delay(500)
        suggestionsResult = Innertube.searchSuggestions(
            body = SearchSuggestionsBody(input = textFieldValue.text)
        )
    }

    val playlistId = remember(textFieldValue.text) {
        runCatching {
            Url(textFieldValue.text).takeIf {
                it.host.endsWith("youtube.com", ignoreCase = true) &&
                    it.segments.lastOrNull()?.equals("playlist", ignoreCase = true) == true
            }?.parameters?.get("list")
        }.getOrNull()
    }

    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
            .asPaddingValues(),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.items.horizontalPadding)
    ) {
        item(
            key = "header",
            contentType = 0
        ) {
            val container = LocalPinnableContainer.current

            DisposableEffect(Unit) {
                val handle = container?.pin()

                onDispose {
                    handle?.release()
                }
            }

            LaunchedEffect(focused) {
                if (!focused) return@LaunchedEffect

                delay(300)
                focusRequester.requestFocus()
            }

            Header(
                titleContent = {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = onTextFieldValueChange,
                        textStyle = typography.xxl.medium.align(TextAlign.Center),
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (textFieldValue.text.isNotEmpty()) onSearch(textFieldValue.text)
                            }
                        ),
                        cursorBrush = SolidColor(colorPalette.text),
                        decorationBox = decorationBox,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                actionsContent = {
                    if (playlistId != null) {
                        val isAlbum = playlistId.startsWith("OLAK5uy_")

                        SecondaryTextButton(
                            text = if (isAlbum) stringResource(R.string.view_album)
                            else stringResource(R.string.view_playlist),
                            onClick = { onViewPlaylist(textFieldValue.text) }
                        )
                    }

                    if (textFieldValue.text.isNotEmpty()) SecondaryTextButton(
                        text = stringResource(R.string.clear),
                        onClick = { onTextFieldValueChange(TextFieldValue()) }
                    )
                }
            )
        }

        items(
            items = history,
            key = SearchQuery::id
        ) { searchQuery ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimensions.items.verticalPadding)
                    .animateItem(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clickable { onSearch(searchQuery.query) }
                        .padding(horizontal = Dimensions.items.horizontalPadding)
                ) {
                    Spacer(
                        modifier = Modifier
                            .size(20.dp)
                            .paint(
                                painter = painterResource(R.drawable.time),
                                colorFilter = ColorFilter.disabled
                            )
                    )

                    BasicText(
                        text = searchQuery.query,
                        style = typography.s.secondary,
                        modifier = Modifier,
                        maxLines = 1
                    )

                    Image(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        colorFilter = ColorFilter.disabled,
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    query {
                                        Database.delete(searchQuery)
                                    }
                                }
                            )
                            .size(20.dp)
                    )

                    Image(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                        colorFilter = ColorFilter.disabled,
                        modifier = Modifier
                            .clickable(
                                indication = ripple(bounded = false),
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onTextFieldValueChange(
                                        TextFieldValue(
                                            text = searchQuery.query,
                                            selection = TextRange(searchQuery.query.length)
                                        )
                                    )
                                }
                            )
                            .rotate(225f)
                            .size(22.dp)
                    )
                }
            }
        }

        suggestionsResult?.getOrNull()?.let { suggestions ->
            items(items = suggestions) { suggestion ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.items.verticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clickable { onSearch(suggestion) }
                            .padding(horizontal = Dimensions.items.horizontalPadding)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(20.dp)
                                .paint(
                                    painter = painterResource(R.drawable.search),
                                    colorFilter = ColorFilter.disabled
                                )
                        )

                        BasicText(
                            text = suggestion,
                            style = typography.s.secondary,
                            modifier = Modifier,
                            maxLines = 1
                        )

                        Image(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            colorFilter = ColorFilter.disabled,
                            modifier = Modifier
                                .clickable(
                                    indication = ripple(bounded = false),
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        onTextFieldValueChange(
                                            TextFieldValue(
                                                text = suggestion,
                                                selection = TextRange(suggestion.length)
                                            )
                                        )
                                    }
                                )
                                .rotate(225f)
                                .size(22.dp)
                        )
                    }
                }
            }
        } ?: suggestionsResult?.exceptionOrNull()?.let {
            item {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicText(
                        text = stringResource(R.string.error_message),
                        style = typography.s.secondary.center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    FloatingActionsContainerWithScrollToTop(lazyListState = lazyListState)
}
