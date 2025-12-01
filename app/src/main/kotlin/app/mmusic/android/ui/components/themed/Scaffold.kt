package app.mmusic.android.ui.components.themed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Down
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Up
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import app.mmusic.android.R
import app.mmusic.android.preferences.UIStatePreferences
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.core.ui.utils.isLandscape
import kotlinx.collections.immutable.toImmutableList
sealed interface TabVisualStyle {
    object Default : TabVisualStyle
    object Pill : TabVisualStyle
}

@Composable
fun Scaffold(
    key: String,
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    tabColumnContent: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    showNavigationBar: Boolean = true,
    tabVisualStyle: TabVisualStyle = TabVisualStyle.Default,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    animateContent: Boolean = true,
    content: @Composable AnimatedVisibilityScope.(Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hiddenTabs by UIStatePreferences.mutableTabStateOf(key)

    if (isLandscape) {
        if (showNavigationBar) {
            Row(
                modifier = modifier
                    .background(colorPalette.background0)
                    .fillMaxSize()
            ) {
                NavigationRail(
                    topIconButtonId = topIconButtonId,
                    onTopIconButtonClick = onTopIconButtonClick,
                    tabIndex = tabIndex,
                    onTabIndexChange = onTabChange,
                    hiddenTabs = hiddenTabs,
                    setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                    tabsEditingTitle = tabsEditingTitle,
                    content = tabColumnContent
                )

                AnimatedContent(
                    targetState = tabIndex,
                    transitionSpec = {
                        val slideDirection = if (targetState > initialState) Up else Down
                        val animationSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = Spring.StiffnessLow,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )

                        if (animateContent) {
                            ContentTransform(
                                targetContentEnter = slideIntoContainer(slideDirection, animationSpec),
                                initialContentExit = slideOutOfContainer(slideDirection, animationSpec),
                                sizeTransform = null
                            )
                        } else {
                            ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                initialContentExit = ExitTransition.None,
                                sizeTransform = null
                            )
                        }
                    },
                    content = content,
                    label = "",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                )
            }
        } else {
            AnimatedVisibility(visible = true, label = "") {
                content(tabIndex)
            }
        }
    } else {
        Column(
            modifier = modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            if (showNavigationBar) {
                if (animateContent) {
                    AnimatedContent(
                        targetState = tabIndex,
                        transitionSpec = {
                            val slideDirection = if (targetState > initialState) Up else Down
                            val animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = Spring.StiffnessLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )

                            ContentTransform(
                                targetContentEnter = slideIntoContainer(slideDirection, animationSpec),
                                initialContentExit = slideOutOfContainer(slideDirection, animationSpec),
                                sizeTransform = null
                            )
                        },
                        content = content,
                        label = "",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                } else {
                    AnimatedVisibility(visible = true, label = "") {
                        content(tabIndex)
                    }
                }
            } else {
                AnimatedVisibility(visible = true, label = "") {
                    content(tabIndex)
                }
            }

            if (showNavigationBar) {
                NavigationBar(
                    topIconButtonId = topIconButtonId,
                    onTopIconButtonClick = onTopIconButtonClick,
                    tabIndex = tabIndex,
                    onTabIndexChange = onTabChange,
                    hiddenTabs = hiddenTabs,
                    setHiddenTabs = { hiddenTabs = it.toImmutableList() },
                    tabsEditingTitle = tabsEditingTitle,
                    content = tabColumnContent,
                    tabVisualStyle = tabVisualStyle
                )
            }
        }
    }
}
