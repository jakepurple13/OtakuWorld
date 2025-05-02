package com.programmersbox.kmpuiviews.utils

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import org.koin.compose.koinInject

@Composable
fun HideSystemBarsWhileOnScreen() {
    val changingSettingsRepository: ChangingSettingsRepository = koinInject()

    LifecycleResumeEffect(Unit) {
        changingSettingsRepository.showInsets.tryEmit(false)
        onPauseOrDispose { changingSettingsRepository.showInsets.tryEmit(true) }
    }
}

@Composable
fun HideNavBarWhileOnScreen() {
    val changingSettingsRepository: ChangingSettingsRepository = koinInject()

    LifecycleResumeEffect(Unit) {
        changingSettingsRepository.showNavBar.tryEmit(false)
        onPauseOrDispose { changingSettingsRepository.showNavBar.tryEmit(true) }
    }
}