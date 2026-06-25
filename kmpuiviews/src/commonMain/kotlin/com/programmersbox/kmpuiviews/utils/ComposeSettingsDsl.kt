package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope
import com.programmersbox.kmpuiviews.presentation.settings.search.SettingsSearchItem

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

    // ── New: search registry ─────────────────────────────────
    var searchItems: () -> List<SettingsSearchItem> = { emptyList() }

    fun searchItems(block: () -> List<SettingsSearchItem>) {
        searchItems = block
    }

    // ── New: per-section injection ───────────────────────────
    var quickActionsSettings: CategoryGroupScope.() -> Unit = {}

    fun quickActionsSettings(block: CategoryGroupScope.() -> Unit) {
        quickActionsSettings = block
    }

    var librarySettings: CategoryGroupScope.() -> Unit = {}

    fun librarySettings(block: CategoryGroupScope.() -> Unit) {
        librarySettings = block
    }

    var discoverSettings: CategoryGroupScope.() -> Unit = {}

    fun discoverSettings(block: CategoryGroupScope.() -> Unit) {
        discoverSettings = block
    }

    var sourcesSettings: CategoryGroupScope.() -> Unit = {}

    fun sourcesSettings(block: CategoryGroupScope.() -> Unit) {
        sourcesSettings = block
    }

    var integrationsSettings: CategoryGroupScope.() -> Unit = {}

    fun integrationsSettings(block: CategoryGroupScope.() -> Unit) {
        integrationsSettings = block
    }

    var appearanceSettings: @Composable () -> Unit = {}

    fun appearanceSettings(block: @Composable () -> Unit) {
        appearanceSettings = block
    }

    var behaviorSettings: @Composable () -> Unit = {}

    fun behaviorSettings(block: @Composable () -> Unit) {
        behaviorSettings = block
    }

    var layoutSettings: @Composable () -> Unit = {}

    fun layoutSettings(block: @Composable () -> Unit) {
        layoutSettings = block
    }

    var contentReadingSettings: @Composable () -> Unit = {}

    fun contentReadingSettings(block: @Composable () -> Unit) {
        contentReadingSettings = block
    }

    var dataSettings: @Composable () -> Unit = {}

    fun dataSettings(block: @Composable () -> Unit) {
        dataSettings = block
    }

    var aboutSettings: CategoryGroupScope.() -> Unit = {}

    fun aboutSettings(block: CategoryGroupScope.() -> Unit) {
        aboutSettings = block
    }
}
