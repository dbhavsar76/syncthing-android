package com.nutomic.syncthingandroid.settings

import android.content.Intent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import com.nutomic.syncthingandroid.R
import com.nutomic.syncthingandroid.activities.LicenseActivity
import me.zhanghai.compose.preference.Preference


fun EntryProviderScope<SettingsRoute>.settingsAboutEntry() {
    entry<SettingsRoute.About> {
        SettingsAboutScreen()
    }
}


@Composable
fun SettingsAboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    SettingsScaffold(
        title = stringResource(R.string.category_about),
    ) {
        Preference(
            title = { Text(stringResource(R.string.app_version_title)) },
            summary = { Text("TODO: app version") },
        )
        Preference(
            title = { Text(stringResource(R.string.syncthing_version_title)) },
            summary = { Text("TODO: st version") },
        )
        Preference(
            title = { Text(stringResource(R.string.syncthing_database_size)) },
            summary = { Text("TODO: db size") },
        )
        Preference(
            title = { Text(stringResource(R.string.os_open_file_limit)) },
            summary = { Text("TODO: open file limit") },
        )

        val stForumUri = stringResource(R.string.syncthing_forum_url)
        Preference(
            title = { Text(stringResource(R.string.syncthing_forum_title)) },
            summary = { Text(stringResource(R.string.syncthing_forum_summary)) },
            onClick = { uriHandler.openUri(stForumUri) },
        )

        val stPrivacyPolicyUri = stringResource(R.string.privacy_policy_url)
        Preference(
            title = { Text(stringResource(R.string.privacy_title)) },
            summary = { Text(stringResource(R.string.privacy_summary)) },
            onClick = { uriHandler.openUri(stPrivacyPolicyUri) },
        )

        Preference(
            title = { Text(stringResource(R.string.open_source_licenses_title)) },
            summary = { Text(stringResource(R.string.open_source_licenses_summary)) },
            onClick = {
                val intent = Intent(context, LicenseActivity::class.java)
                context.startActivity(intent)
            },
        )
    }
}
