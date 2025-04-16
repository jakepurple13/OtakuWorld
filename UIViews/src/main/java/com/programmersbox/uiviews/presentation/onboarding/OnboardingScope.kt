package com.programmersbox.uiviews.presentation.onboarding

import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

interface OnboardingScope {
    fun item(content: @Composable () -> Unit)
}

internal class OnboardingScopeImpl(
    content: OnboardingScope.() -> Unit = {},
) : OnboardingScope {
    val intervals: MutableIntervalList<@Composable () -> Unit> = MutableIntervalList()

    init {
        apply(content)
    }

    override fun item(content: @Composable () -> Unit) {
        intervals.addInterval(1, content)
    }
}