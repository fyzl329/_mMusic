package app.mmusic.android.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.mmusic.android.R
import app.mmusic.android.ui.components.themed.Scaffold
import app.mmusic.android.ui.components.themed.TabVisualStyle
import app.mmusic.android.ui.screens.GlobalRoutes
import app.mmusic.android.ui.screens.Route
import app.mmusic.android.utils.medium
import app.mmusic.android.utils.secondary
import app.mmusic.compose.persist.PersistMapCleanup
import app.mmusic.compose.routing.RouteHandler
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
@Route
@Composable
fun SearchScreen(
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit
) {
    RouteHandler {
        GlobalRoutes()

        Content {
            SearchContent(
                initialTextInput = initialTextInput,
                onSearch = onSearch,
                onViewPlaylist = onViewPlaylist,
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                showNavigationBar = true,
                shouldAutoFocus = child == null
            )
        }
    }
}

@Composable
fun SearchContent(
    initialTextInput: String,
    onSearch: (String) -> Unit,
    onViewPlaylist: (String) -> Unit,
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    showNavigationBar: Boolean,
    shouldAutoFocus: Boolean
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }

    val (textFieldValue, onTextFieldValueChanged) = rememberSaveable(
        initialTextInput,
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(
            TextFieldValue(
                text = initialTextInput,
                selection = TextRange(initialTextInput.length)
            )
        )
    }

    PersistMapCleanup(prefix = "search/")

    val decorationBox: @Composable (@Composable () -> Unit) -> Unit = { innerTextField ->
        Box {
            AnimatedVisibility(
                visible = textFieldValue.text.isEmpty(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                BasicText(
                    text = stringResource(R.string.search_placeholder),
                    maxLines = 1,
                    style = LocalAppearance.current.typography.xxl.secondary
                )
            }

            innerTextField()
        }
    }

    Scaffold(
        key = "search",
        topIconButtonId = topIconButtonId,
        onTopIconButtonClick = onTopIconButtonClick,
        tabIndex = tabIndex,
        onTabChange = onTabChanged,
        tabColumnContent = {
            tab(0, R.string.online, R.drawable.globe, canHide = false)
            tab(1, R.string.library, R.drawable.library, canHide = false)
        },
        tabVisualStyle = TabVisualStyle.Pill,
        showNavigationBar = true,
        animateContent = false // TODO: consider reintroducing a light transition once UX is stable
    ) { currentTabIndex ->
        saveableStateHolder.SaveableStateProvider(currentTabIndex) {
            when (currentTabIndex) {
                0 -> OnlineSearch(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = onTextFieldValueChanged,
                    onSearch = onSearch,
                    onViewPlaylist = onViewPlaylist,
                    decorationBox = decorationBox,
                    focused = shouldAutoFocus
                )

                1 -> LocalSongSearch(
                    textFieldValue = textFieldValue,
                    onTextFieldValueChange = onTextFieldValueChanged,
                    decorationBox = decorationBox
                )
            }
        }
    }
}
