package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.all.AllViewModel
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.recent.RecentViewModel
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels: Module = module {
    viewModelOf(::IncognitoViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::FavoriteViewModel)
    viewModelOf(::GlobalSearchViewModel)
    viewModelOf(::RecentViewModel)
    viewModelOf(::AllViewModel)
}