package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels: Module = module {
    viewModelOf(::IncognitoViewModel)
    viewModelOf(::NotificationSettingsViewModel)
}