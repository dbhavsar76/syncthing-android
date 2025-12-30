package com.nutomic.syncthingandroid.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nutomic.syncthingandroid.activities.SyncthingActivity
import com.nutomic.syncthingandroid.theme.ApplicationTheme

class SettingsActivity : SyncthingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val routeStr = intent.getStringExtra(EXTRA_START_DESTINATION)
        val startDestination: SettingsRoute = SettingsRoute.fromString(routeStr)

        setContent {
            ApplicationTheme {
                SettingsNavDisplay(
                    startDestination = startDestination,
                    onFinishActivity = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_START_DESTINATION = "com.nutomic.syncthingandroid.settings.EXTRA_START_DESTINATION"
    }
}
