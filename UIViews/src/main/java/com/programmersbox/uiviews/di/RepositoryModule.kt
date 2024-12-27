package com.programmersbox.uiviews.di

import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.uiviews.repository.ChangingSettingsRepository
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import org.koin.core.module.Module

fun Module.repository() {
    single { SourceRepository() }
    single { CurrentSourceRepository() }
    single { ChangingSettingsRepository() }
}