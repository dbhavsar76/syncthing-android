package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import com.nutomic.syncthingandroid.R

fun EntryProviderScope<SettingsRoute>.settingsRootEntry(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit
) {
    entry<SettingsRoute.Root> {
        SettingsRootScreen(backstack, onBack)
    }
}

@Composable
fun SettingsRootScreen(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit = {},
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
    ) {
        TextButton(onClick = { backstack.add(SettingsRoute.RunConditions) }) {
            Text("RunConditions")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.UserInterface) }) {
            Text("UserInterface")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.Behavior) }) {
            Text("Behavior")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.SyncthingOptions) }) {
            Text("SyncthingOptions")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.ImportExport) }) {
            Text("ImportExport")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.Troubleshooting) }) {
            Text("Troubleshooting")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.Experimental) }) {
            Text("Experimental")
        }
        TextButton(onClick = { backstack.add(SettingsRoute.About) }) {
            Text("About")
        }
    }
}
