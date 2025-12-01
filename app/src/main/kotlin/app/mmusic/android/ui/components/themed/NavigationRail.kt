package app.mmusic.android.ui.components.themed

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.R
import app.mmusic.android.ui.screens.settings.SwitchSettingsEntry
import app.mmusic.android.utils.center
import app.mmusic.android.utils.color
import app.mmusic.android.utils.semiBold
import app.mmusic.core.ui.ColorPalette
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.core.ui.Typography
import app.mmusic.core.ui.utils.isLandscape
import app.mmusic.core.ui.utils.roundedShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
class TabsBuilder @PublishedApi internal constructor() {
    companion object {
        @Composable
        inline fun rememberTabs(crossinline content: TabsBuilder.() -> Unit) = rememberSaveable(
            saver = listSaver(
                save = { it },
                restore = { it.toImmutableList() }
            )
        ) {
            TabsBuilder().apply(content).tabs.values.toImmutableList()
        }
    }

    @PublishedApi
    internal val tabs = mutableMapOf<String, Tab>()

    fun tab(
        key: Int,
        @StringRes
        title: Int,
        @DrawableRes
        icon: Int,
        canHide: Boolean = true
    ): Tab = tab(key.toString(), title, icon, canHide)

    fun tab(
        key: String,
        @StringRes
        title: Int,
        @DrawableRes
        icon: Int,
        canHide: Boolean = true
    ): Tab {
        require(key.isNotBlank()) { "key cannot be blank" }
        require(!tabs.containsKey(key)) { "key already exists" }
        require(icon != 0) { "icon is 0" }

        val ret = Tab.ResourcesTab(
            key = key,
            titleRes = title,
            icon = icon,
            canHide = canHide
        )
        tabs += key to ret
        return ret
    }

    fun tab(
        key: Int,
        title: String,
        @DrawableRes
        icon: Int,
        canHide: Boolean = true
    ): Tab = tab(key.toString(), title, icon, canHide)

    fun tab(
        key: String,
        title: String,
        @DrawableRes
        icon: Int,
        canHide: Boolean = true
    ): Tab {
        require(key.isNotBlank()) { "key cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(!tabs.containsKey(key)) { "key already exists" }
        require(icon != 0) { "icon is 0" }

        val ret = Tab.StaticTab(
            key = key,
            titleText = title,
            icon = icon,
            canHide = canHide
        )
        tabs += key to ret
        return ret
    }
}

@Parcelize
sealed class Tab : Parcelable {
    abstract val key: String

    @IgnoredOnParcel
    abstract val title: @Composable () -> String

    @get:DrawableRes
    abstract val icon: Int
    abstract val canHide: Boolean

    data class ResourcesTab(
        override val key: String,
        @param:StringRes
        private val titleRes: Int,
        @param:DrawableRes
        override val icon: Int,
        override val canHide: Boolean
    ) : Tab() {
        @IgnoredOnParcel
        override val title: @Composable () -> String = { stringResource(titleRes) }
    }

    data class StaticTab(
        override val key: String,
        private val titleText: String,
        @param:DrawableRes
        override val icon: Int,
        override val canHide: Boolean
    ) : Tab() {
        @IgnoredOnParcel
        override val title: @Composable () -> String = { titleText }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavigationRail(
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    hiddenTabs: ImmutableList<String>,
    setHiddenTabs: (List<String>) -> Unit,
    content: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs)
) {
    val (colorPalette, typography) = LocalAppearance.current

    val tabs = TabsBuilder.rememberTabs(content)
    val isLandscape = isLandscape

    val paddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
        .asPaddingValues()

    var editing by remember { mutableStateOf(false) }

    if (editing) DefaultDialog(
        onDismiss = { editing = false },
        horizontalPadding = 0.dp
    ) {
        BasicText(
            text = tabsEditingTitle,
            style = typography.s.center.semiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(
                items = tabs,
                key = { it.key }
            ) { tab ->
                SwitchSettingsEntry(
                    title = tab.title(),
                    text = null,
                    isChecked = tab.key !in hiddenTabs,
                    onCheckedChange = {
                        if (!it && hiddenTabs.size == tabs.size - 1) return@SwitchSettingsEntry

                        setHiddenTabs(if (it) hiddenTabs - tab.key else hiddenTabs + tab.key)
                    },
                    isEnabled = tab.canHide && (tab.key in hiddenTabs || hiddenTabs.size < tabs.size - 1)
                )
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
    ) {
        if (topIconButtonId != 0) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .size(
                        width = if (isLandscape) Dimensions.navigationRail.widthLandscape
                        else Dimensions.navigationRail.width,
                        height = Dimensions.items.headerHeight
                    )
            ) {
                Image(
                    painter = painterResource(topIconButtonId),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                    modifier = Modifier
                        .offset(
                            x = if (isLandscape) 0.dp else Dimensions.navigationRail.iconOffset,
                            y = 48.dp
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onTopIconButtonClick)
                        .padding(all = 12.dp)
                        .size(22.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.width(
                if (isLandscape) Dimensions.navigationRail.widthLandscape
                else Dimensions.navigationRail.width
            )
        ) {
            val transition = updateTransition(targetState = tabIndex, label = null)

            tabs.fastForEachIndexed { index, tab ->
                AnimatedVisibility(
                    visible = tabIndex == index || tab.key !in hiddenTabs,
                    label = ""
                ) {
                    val dothAlpha by transition.animateFloat(label = "") {
                        if (it == index) 1f else 0f
                    }

                    val textColor by transition.animateColor(label = "") {
                        if (it == index) colorPalette.text else colorPalette.textDisabled
                    }

                    val iconContent: @Composable () -> Unit = {
                        Image(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .vertical(enabled = !isLandscape)
                                .graphicsLayer {
                                    alpha = dothAlpha
                                    translationX = (1f - dothAlpha) * -48.dp.toPx()
                                    rotationZ = if (isLandscape) 0f else -90f
                                }
                                .size(Dimensions.navigationRail.iconOffset * 2)
                        )
                    }

                    val textContent: @Composable () -> Unit = {
                        BasicText(
                            text = tab.title(),
                            style = typography.xs.semiBold.center.color(textColor),
                            modifier = Modifier
                                .vertical(enabled = !isLandscape)
                                .rotate(if (isLandscape) 0f else -90f)
                                .padding(horizontal = 16.dp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2
                        )
                    }

                    val contentModifier = Modifier
                        .clip(24.dp.roundedShape)
                        .combinedClickable(
                            onClick = { onTabIndexChange(index) },
                            onLongClick = { editing = true }
                        )

                    if (isLandscape) Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = contentModifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                    ) {
                        iconContent()
                        textContent()
                    } else Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = contentModifier.padding(horizontal = 8.dp)
                    ) {
                        iconContent()
                        textContent()
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationBar(
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    hiddenTabs: ImmutableList<String>,
    setHiddenTabs: (List<String>) -> Unit,
    content: TabsBuilder.() -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    tabVisualStyle: TabVisualStyle = TabVisualStyle.Default
) {
    val (colorPalette, typography) = LocalAppearance.current
    val tabs = TabsBuilder.rememberTabs(content)
    val visibleTabs = tabs.filterNot { it.canHide && it.key in hiddenTabs }

    val paddingValues = LocalPlayerAwareWindowInsets.current
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .asPaddingValues()

    var editing by remember { mutableStateOf(false) }

    val coercedTabIndex = visibleTabs.firstOrNull { it.key == tabIndex.toString() }
        ?: visibleTabs.firstOrNull()
    if (coercedTabIndex != null && coercedTabIndex.key != tabIndex.toString()) {
        coercedTabIndex.key.toIntOrNull()?.let(onTabIndexChange)
    }

    if (editing) DefaultDialog(
        onDismiss = { editing = false },
        horizontalPadding = 0.dp
    ) {
        BasicText(
            text = tabsEditingTitle,
            style = typography.s.center.semiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(
                items = tabs,
                key = { it.key }
            ) { tab ->
                SwitchSettingsEntry(
                    title = tab.title(),
                    text = null,
                    isChecked = tab.key !in hiddenTabs,
                    onCheckedChange = {
                        if (!it && hiddenTabs.size == tabs.size - 1) return@SwitchSettingsEntry

                        setHiddenTabs(if (it) hiddenTabs - tab.key else hiddenTabs + tab.key)
                    },
                    isEnabled = tab.canHide && (tab.key in hiddenTabs || hiddenTabs.size < tabs.size - 1)
                )
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(colorPalette.background0)
            .padding(paddingValues)
            .height(84.dp)
    ) {
        if (topIconButtonId != 0) {
            Image(
                painter = painterResource(topIconButtonId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onTopIconButtonClick,
                        onLongClick = { editing = true }
                    )
                    .padding(4.dp)
            )
        }

        when (tabVisualStyle) {
            TabVisualStyle.Default -> DefaultTabsRow(
                visibleTabs = visibleTabs.toImmutableList(),
                coercedTabIndexKey = coercedTabIndex?.key,
                tabIndex = tabIndex,
                onTabIndexChange = onTabIndexChange,
                colorPalette = colorPalette,
                typography = typography,
                modifier = Modifier.weight(1f)
            )

            TabVisualStyle.Pill -> PillTabsRow(
                visibleTabs = visibleTabs.toImmutableList(),
                coercedTabIndexKey = coercedTabIndex?.key,
                tabIndex = tabIndex,
                onTabIndexChange = onTabIndexChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DefaultTabsRow(
    visibleTabs: ImmutableList<Tab>,
    coercedTabIndexKey: String?,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    colorPalette: ColorPalette,
    typography: Typography,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        visibleTabs.forEachIndexed { index, tab ->
            val selected = tab.key == (coercedTabIndexKey ?: tabIndex.toString())
            val targetTabIndex = tab.key.toIntOrNull() ?: index

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onTabIndexChange(targetTabIndex) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Image(
                    painter = painterResource(tab.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        if (selected) colorPalette.accent else colorPalette.textSecondary
                    ),
                    modifier = Modifier.size(20.dp)
                )

                BasicText(
                    text = tab.title(),
                    style = typography.xs.copy(
                        color = if (selected) colorPalette.accent else colorPalette.textSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PillTabsRow(
    visibleTabs: ImmutableList<Tab>,
    coercedTabIndexKey: String?,
    tabIndex: Int,
    onTabIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        visibleTabs.forEachIndexed { index, tab ->
            val targetTabIndex = tab.key.toIntOrNull() ?: index
            val selected = tab.key == (coercedTabIndexKey ?: tabIndex.toString())

            SecondaryTextButton(
                text = tab.title(),
                onClick = { onTabIndexChange(targetTabIndex) },
                alternative = !selected
            )
        }
    }
}

fun Modifier.vertical(enabled: Boolean = true) =
    if (enabled)
        layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(maxWidth = Int.MAX_VALUE))
            layout(placeable.height, placeable.width) {
                placeable.place(
                    x = -(placeable.width / 2 - placeable.height / 2),
                    y = -(placeable.height / 2 - placeable.width / 2)
                )
            }
        } else this
