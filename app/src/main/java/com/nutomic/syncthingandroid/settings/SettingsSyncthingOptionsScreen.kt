package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.nutomic.syncthingandroid.R


fun EntryProviderScope<SettingsRoute>.settingsSyncthingOptionsEntry(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit
) {
    entry<SettingsRoute.SyncthingOptions> {
        SettingsSyncthingOptionsScreen(onBack = onBack)
    }
}


@Composable
fun SettingsSyncthingOptionsScreen(onBack: () -> Unit = {}) {
    SettingsScaffold(
        title = stringResource(R.string.category_syncthing_options),
        description = stringResource(R.string.category_syncthing_options_summary),
        onBack = onBack,
    ) {
        Text(stringResource(R.string.category_syncthing_options))
    }
}
