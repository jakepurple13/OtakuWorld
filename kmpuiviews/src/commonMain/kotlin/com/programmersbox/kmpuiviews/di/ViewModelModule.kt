package com.programmersbox.kmpuiviews.di

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.presentation.all.AllViewModel
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenViewModel
import com.programmersbox.kmpuiviews.presentation.recent.RecentViewModel
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportFullListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels: Module = module {
    viewModelOf(::IncognitoViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::FavoriteViewModel)
    viewModelOf(::GlobalSearchViewModel)
    //viewModel { GlobalSearchViewModel(createSavedStateHandle(), get(), get()) }
    viewModelOf(::RecentViewModel)
    viewModelOf(::AllViewModel)
    viewModelOf(::ImportFullListViewModel)
    viewModelOf(::ImportListViewModel)
    viewModelOf(::OtakuListViewModel)
    viewModelOf(::NotificationScreenViewModel)
    viewModel { OtakuCustomListViewModel(get(), get<DataStoreHandling>().showBySource) }
}