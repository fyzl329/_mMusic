package app.mmusic.android.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.mmusic.android.R
import app.mmusic.android.ui.screens.Route

@Route
@Composable
fun StorageSettings() {
    SettingsCategoryScreen(title = stringResource(R.string.storage)) {
        CacheSettingsGroups()
        DatabaseSettingsGroups()
    }
}
