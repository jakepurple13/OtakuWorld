package com.programmersbox.uiviews.di

import com.programmersbox.uiviews.datastore.DataStoreHandling
import com.programmersbox.uiviews.presentation.all.AllViewModel
import com.programmersbox.uiviews.presentation.details.DetailsViewModel
import com.programmersbox.uiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.uiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.uiviews.presentation.history.HistoryViewModel
import com.programmersbox.uiviews.presentation.lists.ImportListViewModel
import com.programmersbox.uiviews.presentation.lists.OtakuCustomListViewModel
import com.programmersbox.uiviews.presentation.lists.OtakuListViewModel
import com.programmersbox.uiviews.presentation.notifications.NotificationScreenViewModel
import com.programmersbox.uiviews.presentation.recent.RecentViewModel
import com.programmersbox.uiviews.presentation.settings.ExtensionListViewModel
import com.programmersbox.uiviews.presentation.settings.MoreInfoViewModel
import com.programmersbox.uiviews.presentation.settings.MoreSettingsViewModel
import com.programmersbox.uiviews.presentation.settings.NotificationViewModel
import com.programmersbox.uiviews.presentation.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf

fun Module.viewModels() {
    viewModel { OtakuCustomListViewModel(get(), get<DataStoreHandling>().showBySource) }
    viewModelOf(::OtakuListViewModel)
    viewModelOf(::RecentViewModel)
    viewModelOf(::NotificationViewModel)
    viewModelOf(::MoreSettingsViewModel)
    viewModelOf(::ExtensionListViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::GlobalSearchViewModel)
    viewModelOf(::FavoriteViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::AllViewModel)
    viewModelOf(::ImportListViewModel)
    viewModelOf(::NotificationScreenViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MoreInfoViewModel)
}