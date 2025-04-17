package com.programmersbox.uiviews.di

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.uiviews.presentation.all.AllViewModel
import com.programmersbox.uiviews.presentation.details.DetailsViewModel
import com.programmersbox.uiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.uiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.uiviews.presentation.history.HistoryViewModel
import com.programmersbox.uiviews.presentation.lists.OtakuCustomListViewModel
import com.programmersbox.uiviews.presentation.lists.OtakuListViewModel
import com.programmersbox.uiviews.presentation.lists.imports.ImportFullListViewModel
import com.programmersbox.uiviews.presentation.lists.imports.ImportListViewModel
import com.programmersbox.uiviews.presentation.notifications.NotificationScreenViewModel
import com.programmersbox.uiviews.presentation.recent.RecentViewModel
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateViewModel
import com.programmersbox.uiviews.presentation.settings.extensions.ExtensionListViewModel
import com.programmersbox.uiviews.presentation.settings.extensions.IncognitoViewModel
import com.programmersbox.uiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.uiviews.presentation.settings.updateprerelease.PrereleaseViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.MoreInfoViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.NotificationViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.SettingsViewModel
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
    viewModelOf(::ImportFullListViewModel)
    viewModelOf(::NotificationScreenViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::MoreInfoViewModel)
    viewModelOf(::PrereleaseViewModel)
    viewModelOf(::DownloadStateViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::IncognitoViewModel)
}