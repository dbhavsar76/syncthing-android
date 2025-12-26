package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.nutomic.syncthingandroid.R


fun EntryProviderScope<SettingsRoute>.settingsRunConditionsEntry(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit
) {
    entry<SettingsRoute.RunConditions> {
        SettingsRunConditionsScreen(onBack = onBack)
    }
}


@Composable
fun SettingsRunConditionsScreen(onBack: () -> Unit = {}) {
    SettingsScaffold(
        title = stringResource(R.string.run_conditions_title),
        description = stringResource(R.string.run_conditions_summary),
        onBack = onBack,
    ) {
        Text(stringResource(R.string.run_conditions_title))
    }
}
