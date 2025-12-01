package app.mmusic.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

object Dimensions {
    object Thumbnails {
        val album = 108.dp
        val artist = 92.dp
        val song = 54.dp
        val playlist = album

        val player = Player

        object Player {
            val song
                @Composable get() = with(LocalConfiguration.current) {
                    minOf(screenHeightDp, screenWidthDp)
                }.dp
        }
    }

    val thumbnails = Thumbnails

    object Items {
        val moodHeight = 64.dp
        val headerHeight = 140.dp
        val collapsedPlayerHeight = 64.dp

        val verticalPadding = 16.dp
        val horizontalPadding = 16.dp
        val alternativePadding = 20.dp
        val doubleVerticalPadding = 32.dp

        val gap = 8.dp

        val iconSmall = 20.dp
        val iconMedium = 22.dp
        val iconLarge = 24.dp
    }

    val items = Items

    object NavigationRail {
        val width = 60.dp
        val widthLandscape = 120.dp
        val iconOffset = 6.dp
    }

    val navigationRail = NavigationRail
}
