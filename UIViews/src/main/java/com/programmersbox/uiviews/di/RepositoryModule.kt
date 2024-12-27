package com.programmersbox.uiviews.di

import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.uiviews.ChangingSettingsRepository
import com.programmersbox.uiviews.CurrentSourceRepository
import org.koin.core.module.Module

fun Module.repository() {
    single { SourceRepository() }
    single { CurrentSourceRepository() }
    single { ChangingSettingsRepository() }
}