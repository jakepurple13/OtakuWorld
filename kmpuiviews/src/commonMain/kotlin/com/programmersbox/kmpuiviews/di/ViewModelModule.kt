package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.all.AllViewModel
import com.programmersbox.kmpuiviews.presentation.details.DetailsViewModel
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.history.HistoryViewModel
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenViewModel
import com.programmersbox.kmpuiviews.presentation.recent.RecentViewModel
import com.programmersbox.kmpuiviews.presentation.recommendations.RecommendationViewModel
import com.programmersbox.kmpuiviews.presentation.settings.SettingViewModel
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.downloadstate.DownloadStateViewModel
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.incognito.IncognitoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportFullListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.imports.ImportListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.moresettings.MoreSettingsViewModel
import com.programmersbox.kmpuiviews.presentation.settings.notifications.NotificationSettingsViewModel
import com.programmersbox.kmpuiviews.presentation.settings.prerelease.PrereleaseViewModel
import com.programmersbox.kmpuiviews.presentation.settings.qrcode.QrCodeScannerViewModel
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.TranslationViewModel
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoViewModel
import com.programmersbox.kmpuiviews.presentation.urlopener.UrlOpenerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModels: Module = module {
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::NotificationScreenViewModel)
    viewModelOf(::PrereleaseViewModel)
    viewModelOf(::ExtensionListViewModel)
    viewModelOf(::IncognitoViewModel)
    viewModelOf(::FavoriteViewModel)
    viewModelOf(::GlobalSearchViewModel)
    viewModelOf(::RecentViewModel)
    viewModelOf(::AllViewModel)
    viewModelOf(::ImportFullListViewModel)
    viewModelOf(::ImportListViewModel)
    viewModelOf(::OtakuListViewModel)
    viewModelOf(::MoreSettingsViewModel)
    viewModelOf(::SettingViewModel)
    viewModelOf(::MoreInfoViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::TranslationViewModel)
    viewModelOf(::DownloadStateViewModel)
    viewModelOf(::AccountInfoViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::QrCodeScannerViewModel)
    viewModelOf(::WorkerInfoViewModel)
    viewModelOf(::RecommendationViewModel)
    viewModelOf(::UrlOpenerViewModel)
    viewModelOf(::OtakuCustomListViewModel)

    includes(platformViewModels())
}

expect fun platformViewModels(): Module