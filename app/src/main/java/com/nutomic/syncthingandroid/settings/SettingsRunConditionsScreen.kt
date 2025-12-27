package com.nutomic.syncthingandroid.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation3.runtime.EntryProviderScope
import com.nutomic.syncthingandroid.R
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.MultiSelectListPreference
import me.zhanghai.compose.preference.PreferenceCategory
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference


fun EntryProviderScope<SettingsRoute>.settingsRunConditionsEntry() {
    entry<SettingsRoute.RunConditions> {
        SettingsRunConditionsScreen()
    }
}


@Composable
fun SettingsRunConditionsScreen() {
    val runOnWifi = remember { mutableStateOf(false) }
    val runOnMeteredWifi = remember { mutableStateOf(false) }
    val runOnSpecifiedSsid = remember { mutableStateOf(false) }
    val specifiedSsids = remember { mutableStateOf(setOf<String>()) }

    val runOnMobileData = remember { mutableStateOf(false) }
    val runOnRoaming = remember { mutableStateOf(false) }

    val powerSourceNames = stringArrayResource(R.array.power_source_entries)
    val powerSourceValues = stringArrayResource(R.array.power_source_values)
    val powerSource = remember { mutableStateOf(powerSourceValues[0]) }

    val respectBatterySaving = remember { mutableStateOf(false) }
    val respectMasterSync = remember { mutableStateOf(false) }
    val flightMode = remember { mutableStateOf(false) }

    val runScheduled = remember { mutableStateOf(false) }
    val syncDuration = remember { mutableStateOf(5) }
    val sleepInterval = remember { mutableStateOf(60) }



    SettingsScaffold(
        title = stringResource(R.string.run_conditions_title),
        description = stringResource(R.string.run_conditions_summary),
    ) {
        PreferenceCategory(
            title = { Text(stringResource(R.string.category_wifi)) },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_wifi_title)) },
            summary = { Text(stringResource(R.string.run_on_wifi_summary)) },
            state = runOnWifi,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_metered_wifi_title)) },
            summary = { Text(stringResource(R.string.run_on_metered_wifi_summary)) },
            state = runOnMeteredWifi,
            enabled = runOnWifi.value,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_whitelisted_wifi_title)) },
            summary = { Text(stringResource(R.string.run_on_whitelisted_wifi_summary)) },
            state = runOnSpecifiedSsid,
            enabled = runOnWifi.value,
        )

        val specifiedSsidSummary = if (specifiedSsids.value.isNotEmpty())
            buildString {
                append(stringResource(R.string.specify_wifi_ssid_whitelist))
                append(": ")
                append(specifiedSsids.value.joinToString(", "))
            }
        else
            stringResource(R.string.wifi_ssid_whitelist_empty)
        MultiSelectListPreference(
            title = { Text(stringResource(R.string.specify_wifi_ssid_whitelist)) },
            summary = { Text(specifiedSsidSummary) },
            state = specifiedSsids,
            enabled = runOnWifi.value && runOnSpecifiedSsid.value,
            values = listOf("ssid1", "ssid2", "ssid3")
        )

        PreferenceCategory(
            title = { Text(stringResource(R.string.category_mobile_data)) }
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_mobile_data_title)) },
            summary = { Text(stringResource(R.string.run_on_mobile_data_summary)) },
            state = runOnMobileData,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_roaming_title)) },
            summary = { Text(stringResource(R.string.run_on_roaming_summary)) },
            state = runOnRoaming,
            enabled = runOnMobileData.value,
        )

        PreferenceCategory(
            title = { Text(stringResource(R.string.category_battery)) },
        )
        ListPreference(
            title = { Text(stringResource(R.string.power_source_title)) },
            summary = { Text(powerSourceNames[powerSourceValues.indexOf(powerSource.value)]) },
            state = powerSource,
            values = powerSourceValues.toList(),
            valueToText = { value -> AnnotatedString(powerSourceNames[powerSourceValues.indexOf(value)]) }
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.respect_battery_saving_title)) },
            summary = { Text(stringResource(R.string.respect_battery_saving_summary)) },
            state = respectBatterySaving,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.respect_master_sync_title)) },
            summary = { Text(stringResource(R.string.respect_master_sync_summary)) },
            state = respectMasterSync,
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_in_flight_mode_title)) },
            summary = { Text(stringResource(R.string.run_in_flight_mode_summary)) },
            state = flightMode,
        )

        PreferenceCategory(
            title = { Text(stringResource(R.string.category_schedule)) },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.run_on_time_schedule_title)) },
            summary = { Text(stringResource(R.string.run_on_time_schedule_summary)) },
            state = runScheduled,
        )
        TextFieldPreference(
            title = { Text(stringResource(R.string.sync_duration_minutes_title)) },
            summary = { Text(stringResource(R.string.sync_duration_minutes_summary, syncDuration.value)) },
            state = syncDuration,
            textToValue = { text -> text.toIntOrNull() ?: syncDuration.value}
        )
        TextFieldPreference(
            title = { Text(stringResource(R.string.sleep_interval_minutes_title)) },
            summary = { Text(stringResource(R.string.sync_duration_minutes_summary, sleepInterval.value)) },
            state = sleepInterval,
            textToValue = { text -> text.toIntOrNull() ?: sleepInterval.value}
        )
    }
}
