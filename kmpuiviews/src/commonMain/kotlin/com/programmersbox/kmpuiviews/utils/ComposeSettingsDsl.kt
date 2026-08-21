package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroupScope
import com.programmersbox.kmpuiviews.presentation.onboarding.OnboardingScope

class ComposeSettingsDsl {
    var generalSettings: @Composable () -> Unit = {}
        private set
    var viewSettings: CategoryGroupScope.() -> Unit = {}
        private set
    var playerSettings: @Composable () -> Unit = {}
        private set
    var onboardingSettings: OnboardingScope.() -> Unit = {}
        private set

    fun generalSettings(block: @Composable () -> Unit) {
        val previous = generalSettings
        generalSettings = {
            previous()
            block()
        }
    }

    fun viewSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = viewSettings
        viewSettings = {
            previous()
            block()
        }
    }

    fun playerSettings(block: @Composable () -> Unit) {
        val previous = playerSettings
        playerSettings = {
            previous()
            block()
        }
    }

    fun onboardingSettings(block: OnboardingScope.() -> Unit) {
        val previous = onboardingSettings
        onboardingSettings = {
            previous()
            block()
        }
    }

    // ── New: per-section injection ───────────────────────────
    var quickActionsSettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun quickActionsSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = quickActionsSettings
        quickActionsSettings = {
            previous()
            block()
        }
    }

    var librarySettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun librarySettings(block: CategoryGroupScope.() -> Unit) {
        val previous = librarySettings
        librarySettings = {
            previous()
            block()
        }
    }

    var discoverSettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun discoverSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = discoverSettings
        discoverSettings = {
            previous()
            block()
        }
    }

    var sourcesSettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun sourcesSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = sourcesSettings
        sourcesSettings = {
            previous()
            block()
        }
    }

    var integrationsSettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun integrationsSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = integrationsSettings
        integrationsSettings = {
            previous()
            block()
        }
    }

    var appearanceSettings: @Composable () -> Unit = {}
        private set

    fun appearanceSettings(block: @Composable () -> Unit) {
        val previous = appearanceSettings
        appearanceSettings = {
            previous()
            block()
        }
    }

    var behaviorSettings: @Composable () -> Unit = {}
        private set

    fun behaviorSettings(block: @Composable () -> Unit) {
        val previous = behaviorSettings
        behaviorSettings = {
            previous()
            block()
        }
    }

    var layoutSettings: @Composable () -> Unit = {}
        private set

    fun layoutSettings(block: @Composable () -> Unit) {
        val previous = layoutSettings
        layoutSettings = {
            previous()
            block()
        }
    }

    var contentReadingSettings: @Composable () -> Unit = {}
        private set

    fun contentReadingSettings(block: @Composable () -> Unit) {
        val previous = contentReadingSettings
        contentReadingSettings = {
            previous()
            block()
        }
    }

    var dataSettings: @Composable () -> Unit = {}
        private set

    fun dataSettings(block: @Composable () -> Unit) {
        val previous = dataSettings
        dataSettings = {
            previous()
            block()
        }
    }

    var aboutSettings: CategoryGroupScope.() -> Unit = {}
        private set

    fun aboutSettings(block: CategoryGroupScope.() -> Unit) {
        val previous = aboutSettings
        aboutSettings = {
            previous()
            block()
        }
    }
}

operator fun (ComposeSettingsDsl.() -> Unit).plus(
    other: ComposeSettingsDsl.() -> Unit,
): ComposeSettingsDsl.() -> Unit {
    return {
        this@plus.invoke(this) // Execute the left side's lambda on the DSL instance
        other.invoke(this)     // Execute the right side's lambda on the DSL instance
    }
}