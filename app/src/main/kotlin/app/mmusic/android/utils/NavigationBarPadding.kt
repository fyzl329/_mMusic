package app.mmusic.android.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import app.mmusic.core.ui.utils.isLandscape

/**
 * Extends the bottom padding to account for the NavigationBar height in portrait mode.
 * The NavigationBar is 84.dp tall and needs to be accounted for in scrollable content's contentPadding.
 */
@Composable
fun WindowInsets.asNavigationBarAwarePaddingValues(): PaddingValues {
    val navigationBarHeightDp = if (isLandscape) 0.dp else 84.dp
    val originalPadding = this.asPaddingValues()
    
    return PaddingValues(
        start = originalPadding.calculateLeftPadding(LocalLayoutDirection.current),
        end = originalPadding.calculateRightPadding(LocalLayoutDirection.current),
        top = originalPadding.calculateTopPadding(),
        bottom = originalPadding.calculateBottomPadding() + navigationBarHeightDp
    )
}
