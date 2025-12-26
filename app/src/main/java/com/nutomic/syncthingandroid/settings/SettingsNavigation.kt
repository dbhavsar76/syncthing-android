package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable
sealed interface SettingsRoute : NavKey {

    @Serializable
    data object Root : SettingsRoute

    @Serializable
    data object RunConditions : SettingsRoute
    @Serializable
    data object UserInterface : SettingsRoute
    @Serializable
    data object Behavior : SettingsRoute
    @Serializable
    data object SyncthingOptions : SettingsRoute
    @Serializable
    data object ImportExport : SettingsRoute
    @Serializable
    data object Troubleshooting : SettingsRoute
    @Serializable
    data object Experimental : SettingsRoute
    @Serializable
    data object About : SettingsRoute

}

@Composable
fun rememberSettingsNavBackStack(startDestination: SettingsRoute): NavBackStack<SettingsRoute> {
    return rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        val initialRouts = listOfNotNull(
            SettingsRoute.Root,
            startDestination.takeIf { it != SettingsRoute.Root }
        ).toMutableStateList()
        NavBackStack(initialRouts)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNavDisplay(
    startDestination: SettingsRoute = SettingsRoute.Root,
    onFinishActivity: () -> Unit = {}
) {
    val backStack = rememberSettingsNavBackStack(startDestination)
    val onBack: () -> Unit = {
        if (backStack.size == 1) {
            onFinishActivity()
        } else {
            backStack.removeLastOrNull()
        }
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = settingsNavEntryProvider(backStack, onBack),
        onBack = onBack,
    )
}

fun settingsNavEntryProvider(
    backstack: NavBackStack<SettingsRoute>,
    onBack: () -> Unit,
) = entryProvider<SettingsRoute>(
    fallback = { key ->
        NavEntry<SettingsRoute>(key) { key ->
            Text("$key")
        }
    }
) {
    settingsRootEntry(backstack, onBack)
    settingsRunConditionsEntry(backstack, onBack)
    settingsUserInterfaceEntry(backstack, onBack)
    settingsBehaviorEntry(backstack, onBack)
    settingsSyncthingOptionsEntry(backstack, onBack)
    settingsImportExportEntry(backstack, onBack)
    settingsTroubleshootingEntry(backstack, onBack)
    settingsExperimentalEntry(backstack, onBack)
    settingsAboutEntry(backstack, onBack)
}
