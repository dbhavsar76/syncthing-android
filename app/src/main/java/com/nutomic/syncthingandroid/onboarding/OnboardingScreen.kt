package com.nutomic.syncthingandroid.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.nutomic.syncthingandroid.util.LocalPagerState

/**
 * The list of onboarding pages shown in order.
 */
private val onboardingPages = listOf(
    OnboardingPage.WELCOME,
    OnboardingPage.STORAGE_PERMISSION,
    OnboardingPage.BATTERY_OPTIMIZATION,
    OnboardingPage.LOCATION_PERMISSION,
    OnboardingPage.NOTIFICATION_PERMISSION,
    OnboardingPage.KEY_GENERATION,
)

@Composable
fun OnboardingScreen() {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    CompositionLocalProvider(LocalPagerState provides pagerState) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false,
        ) { pageIdx ->
            OnboardingPage(page = onboardingPages[pageIdx])
        }
    }
}
