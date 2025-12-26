package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.nutomic.syncthingandroid.R


fun EntryProviderScope<SettingsRoute>.settingsExperimentalEntry(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit
) {
    entry<SettingsRoute.Experimental> {
        SettingsExperimentalScreen(onBack = onBack)
    }
}


@Composable
fun SettingsExperimentalScreen(onBack: () -> Unit = {}) {
    SettingsScaffold(
        title = stringResource(R.string.category_experimental),
        onBack = onBack,
    ) {
        Text(stringResource(R.string.category_experimental))
    }
}
