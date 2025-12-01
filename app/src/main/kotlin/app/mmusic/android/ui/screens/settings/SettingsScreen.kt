@file:Suppress("TooManyFunctions")

package app.mmusic.android.ui.screens.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.mmusic.android.LocalPlayerAwareWindowInsets
import app.mmusic.android.R
import app.mmusic.android.ui.components.themed.Header
import app.mmusic.android.ui.components.themed.NumberFieldDialog
import app.mmusic.android.ui.components.themed.NavigationRail
import app.mmusic.android.ui.components.themed.Slider
import app.mmusic.android.ui.components.themed.Switch
import app.mmusic.android.ui.components.themed.ValueSelectorDialog
import app.mmusic.android.ui.screens.GlobalRoutes
import app.mmusic.android.ui.screens.Route
import app.mmusic.android.utils.color
import app.mmusic.android.utils.secondary
import app.mmusic.android.utils.semiBold
import app.mmusic.compose.persist.PersistMapCleanup
import app.mmusic.compose.routing.RouteHandler
import app.mmusic.core.ui.Dimensions
import app.mmusic.core.ui.LocalAppearance
import app.mmusic.android.preferences.UIStatePreferences
import app.mmusic.android.ui.components.themed.TabsBuilder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
@Route
@Composable
fun SettingsScreen() {
    RouteHandler {
        GlobalRoutes()

        Content {
            SettingsContent(
                topIconButtonId = 0,
                onTopIconButtonClick = pop,
                showNavigationBar = true
            )
        }
    }
}

@Composable
fun SettingsContent(
    topIconButtonId: Int,
    onTopIconButtonClick: () -> Unit,
    showNavigationBar: Boolean
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val (tabIndex, onTabChanged) = rememberSaveable { mutableIntStateOf(0) }
    var hiddenTabs by UIStatePreferences.mutableTabStateOf("settings")

    PersistMapCleanup("settings/")
    val safeTabIndex = tabIndex.coerceIn(0, 3)
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationRail(
            topIconButtonId = topIconButtonId,
            onTopIconButtonClick = onTopIconButtonClick,
            tabIndex = safeTabIndex,
            onTabIndexChange = { onTabChanged(it.coerceIn(0, 3)) },
            hiddenTabs = hiddenTabs,
            setHiddenTabs = { hiddenTabs = it.toImmutableList() },
            content = {
                tab(0, R.string.appearance, R.drawable.color_palette, canHide = false)
                tab(1, R.string.player, R.drawable.play, canHide = false)
                tab(2, R.string.storage, R.drawable.server, canHide = false)
                tab(3, R.string.other, R.drawable.shapes, canHide = false)
            },
            modifier = Modifier
        )

        Column(modifier = Modifier.weight(1f)) {
            saveableStateHolder.SaveableStateProvider(safeTabIndex) {
                val clamped = safeTabIndex.coerceIn(0, 3)
                when (clamped) {
                    0 -> AppearanceSettings()
                    1 -> PlayerSettings()
                    2 -> StorageSettings()
                    3 -> OtherSettings()
                }
            }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> EnumValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    noinline onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    noinline valueText: @Composable (T) -> String = { it.name },
    noinline trailingContent: (@Composable () -> Unit)? = null
) = ValueSelectorSettingsEntry(
    title = title,
    selectedValue = selectedValue,
    values = enumValues<T>().toList().toImmutableList(),
    onValueSelect = onValueSelect,
    modifier = modifier,
    isEnabled = isEnabled,
    valueText = valueText,
    trailingContent = trailingContent
)

@Composable
fun <T> ValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    valueText: @Composable (T) -> String = { it.toString() },
    trailingContent: (@Composable () -> Unit)? = null
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) ValueSelectorDialog(
        onDismiss = { isShowingDialog = false },
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelect = onValueSelect,
        valueText = valueText
    )

    SettingsEntry(
        modifier = modifier,
        title = title,
        text = text ?: valueText(selectedValue),
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        trailingContent = trailingContent,
        usePadding = usePadding
    )
}

@Composable
fun SwitchSettingsEntry(
    title: String,
    text: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    usePadding: Boolean = true
) = SettingsEntry(
    modifier = modifier,
    title = title,
    text = text,
    onClick = { onCheckedChange(!isChecked) },
    isEnabled = isEnabled,
    usePadding = usePadding
) {
    Switch(isChecked = isChecked)
}

@Composable
fun SliderSettingsEntry(
    title: String,
    text: String,
    state: Float,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onSlide: (Float) -> Unit = { },
    onSlideComplete: () -> Unit = { },
    toDisplay: @Composable (Float) -> String = { it.toString() },
    steps: Int = 0,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    showTicks: Boolean = steps != 0
) = Column(modifier = modifier) {
    SettingsEntry(
        title = title,
        text = "$text (${toDisplay(state)})",
        onClick = {},
        isEnabled = isEnabled,
        usePadding = usePadding
    )

    Slider(
        state = state,
        setState = onSlide,
        onSlideComplete = onSlideComplete,
        range = range,
        steps = steps,
        showTicks = showTicks,
        modifier = Modifier
            .height(Dimensions.Items.doubleVerticalPadding + Dimensions.Items.gap)
            .alpha(if (isEnabled) 1f else 0.5f)
            .let {
                if (usePadding) it.padding(
                    start = Dimensions.Items.doubleVerticalPadding,
                    end = Dimensions.Items.horizontalPadding
                ) else it
            }
            .padding(vertical = Dimensions.Items.verticalPadding)
            .fillMaxWidth()
    )
}

@Composable
inline fun IntSettingsEntry(
    title: String,
    text: String,
    currentValue: Int,
    crossinline setValue: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    defaultValue: Int = 0,
    isEnabled: Boolean = true,
    usePadding: Boolean = true
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) NumberFieldDialog(
        onDismiss = { isShowingDialog = false },
        onAccept = {
            setValue(it)
            isShowingDialog = false
        },
        initialValue = currentValue,
        defaultValue = defaultValue,
        convert = { it.toIntOrNull() },
        range = range
    )

    SettingsEntry(
        modifier = modifier,
        title = title,
        text = text,
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        usePadding = usePadding
    )
}

@Composable
fun SettingsEntry(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isEnabled: Boolean = true,
    usePadding: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) = Row(
    horizontalArrangement = Arrangement.spacedBy(Dimensions.Items.horizontalPadding),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .clip(RoundedCornerShape(25))
        .clickable(enabled = isEnabled, onClick = onClick)
        .alpha(if (isEnabled) 1f else 0.5f)
        .let {
            if (usePadding) it.padding(
                start = Dimensions.Items.doubleVerticalPadding,
                end = Dimensions.Items.horizontalPadding
            ) else it
        }
        .padding(vertical = Dimensions.Items.verticalPadding)
        .fillMaxWidth()
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(modifier = Modifier.weight(1f)) {
        BasicText(
            text = title,
            style = typography.xs.semiBold.copy(color = colorPalette.text)
        )

        if (text != null) BasicText(
            text = text,
            style = typography.xs.semiBold.secondary
        )
    }

    trailingContent?.invoke()
}

@Composable
fun SettingsDescription(
    text: String,
    modifier: Modifier = Modifier,
    important: Boolean = false
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = if (important) typography.xxs.semiBold.color(colorPalette.red)
        else typography.xxs.secondary,
        modifier = modifier
            .padding(start = Dimensions.Items.horizontalPadding)
            .padding(
                horizontal = Dimensions.Items.horizontalPadding,
                vertical = Dimensions.Items.gap
            )
    )
}

@Composable
fun SettingsEntryGroupText(
    title: String,
    modifier: Modifier = Modifier
) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = title,
        style = typography.xs.semiBold.copy(colorPalette.accent),
        modifier = modifier
            .padding(start = Dimensions.Items.horizontalPadding)
            .padding(horizontal = Dimensions.Items.horizontalPadding)
            .semantics { this.text = AnnotatedString(text = title) }
    )
}

@Composable
fun SettingsGroupSpacer(modifier: Modifier = Modifier) =
    Spacer(modifier = modifier.height(Dimensions.Items.verticalPadding))

@Composable
fun SettingsCategoryScreen(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    scrollState: ScrollState? = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(
        modifier = modifier
            .background(colorPalette.background0)
            .fillMaxSize()
            .let { if (scrollState != null) it.verticalScroll(state = scrollState) else it }
            .padding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                    .asPaddingValues()
            )
    ) {
        Header(title = title) {
            description?.let { description ->
                BasicText(
                    text = description,
                    style = typography.s.secondary
                )
                SettingsGroupSpacer()
            }
        }

        content()
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    important: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = Column(modifier = modifier.fillMaxWidth()) {
    Row {
        Column {
            SettingsEntryGroupText(title = title)

            description?.let { description ->
                SettingsDescription(
                    text = description,
                    important = important
                )
            }
        }

        trailingContent?.let {
            Spacer(Modifier.weight(1f))
            it()
        }
    }

    content()

    SettingsGroupSpacer()
}
