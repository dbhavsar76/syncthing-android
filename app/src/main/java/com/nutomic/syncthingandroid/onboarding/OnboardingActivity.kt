package com.nutomic.syncthingandroid.onboarding

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import com.nutomic.syncthingandroid.SyncthingApp
import com.nutomic.syncthingandroid.activities.MainActivity
import com.nutomic.syncthingandroid.activities.ThemedAppCompatActivity
import com.nutomic.syncthingandroid.activities.WebGuiActivity
import com.nutomic.syncthingandroid.service.Constants
import com.nutomic.syncthingandroid.theme.ApplicationTheme
import com.nutomic.syncthingandroid.util.LocalActivityScope
import javax.inject.Inject

class OnboardingActivity : ThemedAppCompatActivity() {

    @Inject
    lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: move to ThemedAppCompatActivity when every activity is compose
        enableEdgeToEdge()
        (application as SyncthingApp).component().inject(this)

        val activityScope = this.lifecycleScope

        setContent {
            ApplicationTheme {
                CompositionLocalProvider(
                    LocalActivityScope provides activityScope,
                ) {
                    OnboardingScreen()
                }
            }
        }
    }

    fun startApp() {
        val startIntoWebGui = prefs.getBoolean(Constants.PREF_START_INTO_WEB_GUI, false)
        val mainIntent = Intent(this, MainActivity::class.java)

        /*
         * In case start_into_web_gui option is enabled, start both activities
         * so that back navigation works as expected.
         */
        if (startIntoWebGui) {
            startActivities(arrayOf(
                mainIntent,
                Intent(this, WebGuiActivity::class.java)
            ))
        } else {
            startActivity(mainIntent)
        }
        finish()
    }
}
