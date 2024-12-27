package com.programmersbox.uiviews.di

import com.programmersbox.uiviews.all.AllViewModel
import com.programmersbox.uiviews.details.DetailsViewModel
import com.programmersbox.uiviews.favorite.FavoriteViewModel
import com.programmersbox.uiviews.globalsearch.GlobalSearchViewModel
import com.programmersbox.uiviews.history.HistoryViewModel
import com.programmersbox.uiviews.lists.ImportListViewModel
import com.programmersbox.uiviews.lists.OtakuCustomListViewModel
import com.programmersbox.uiviews.lists.OtakuListViewModel
import com.programmersbox.uiviews.notifications.NotificationScreenViewModel
import com.programmersbox.uiviews.recent.RecentViewModel
import com.programmersbox.uiviews.settings.ExtensionListViewModel
import com.programmersbox.uiviews.settings.MoreSettingsViewModel
import com.programmersbox.uiviews.settings.NotificationViewModel
import com.programmersbox.uiviews.settings.SettingsViewModel
import com.programmersbox.uiviews.utils.datastore.DataStoreHandling
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
}