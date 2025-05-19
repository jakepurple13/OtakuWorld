package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.components.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope

class ComposeSettingsDsl {
    //TODO: Turn back to internal once settings move to kmpuiviews
    var generalSettings: @Composable () -> Unit = {}
    var viewSettings: CategoryGroupScope.() -> Unit = {}
    var playerSettings: @Composable () -> Unit = {}

    var onboardingSettings: OnboardingScope.() -> Unit = {}

    fun generalSettings(block: @Composable () -> Unit) {
        generalSettings = block
    }

    fun viewSettings(block: CategoryGroupScope.() -> Unit) {
        viewSettings = block
    }

    fun playerSettings(block: @Composable () -> Unit) {
        playerSettings = block
    }

    fun onboardingSettings(block: OnboardingScope.() -> Unit) {
        onboardingSettings = block
    }
}