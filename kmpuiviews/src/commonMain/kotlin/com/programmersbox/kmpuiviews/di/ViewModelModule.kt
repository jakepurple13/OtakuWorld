package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.all.AllViewModel
import com.programmersbox.kmpuiviews.presentation.bookmarks.BookmarkChaptersViewModel
import com.programmersbox.kmpuiviews.presentation.details.DetailsViewModel
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryDetailViewModel
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryFormViewModel
import com.programmersbox.kmpuiviews.presentation.dictionary.DictionaryListViewModel
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.history.HistoryViewModel
import com.programmersbox.kmpuiviews.presentation.notes.AllNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notes.DetailsNotesViewModel
import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenViewModel
import com.programmersbox.kmpuiviews.presentation.recent.RecentViewModel
import com.programmersbox.kmpuiviews.presentation.settings.SettingViewModel
import com.programmersbox.kmpuiviews.presentation.settings.accountinfo.AccountInfoViewModel
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.BackupWizardViewModel
import com.programmersbox.kmpuiviews.presentation.settings.backuprestore.RestoreWizardViewModel
import com.programmersbox.kmpuiviews.presentation.settings.downloadstate.DownloadStateViewModel
import com.programmersbox.kmpuiviews.presentation.settings.exceptions.ExceptionViewModel
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
import com.programmersbox.kmpuiviews.presentation.settings.translationmodels.TranslationViewModel
import com.programmersbox.kmpuiviews.presentation.settings.workerinfo.WorkerInfoViewModel
import com.programmersbox.kmpuiviews.presentation.urlopener.UrlOpenerViewModel
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.utils.Backup
import io.github.vinceglb.filekit.PlatformFile
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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
    viewModel { MoreSettingsViewModel(get(), getAll()) }
    viewModel {
        BackupWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            listDao = get(),
            resultsFlow = get<BackgroundWorkHandler>().backupResultsFlow(),
            startBackup = { file, keys, listIds -> get<BackgroundWorkHandler>().startBackup(file, keys, listIds) },
        )
    }
    viewModel {
        RestoreWizardViewModel<PlatformFile>(
            uiInfos = getAll(),
            peekZip = { file -> get<Backup>().peekBackup(file, getAll()) },
            peekListContents = { file -> get<Backup>().peekListContents(file) },
            resultsFlow = get<BackgroundWorkHandler>().restoreResultsFlow(),
            startRestore = { file, keys, listIds -> get<BackgroundWorkHandler>().startRestore(file, keys, listIds) },
        )
    }
    viewModelOf(::SettingViewModel)
    viewModelOf(::MoreInfoViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::TranslationViewModel)
    viewModelOf(::DownloadStateViewModel)
    viewModel {
        AccountInfoViewModel(
            itemDao = get(),
            heatMapDao = get(),
            activityDao = get(),
            authManager = get(),
            providers = getAll()
        )
    }
    viewModelOf(::HistoryViewModel)
    viewModelOf(::WorkerInfoViewModel)
    viewModelOf(::UrlOpenerViewModel)
    viewModelOf(::OtakuCustomListViewModel)
    viewModelOf(::ExceptionViewModel)
    viewModelOf(::BookmarkChaptersViewModel)
    viewModelOf(::DetailsNotesViewModel)
    viewModelOf(::AllNotesViewModel)
    viewModelOf(::DictionaryListViewModel)
    viewModelOf(::DictionaryDetailViewModel)
    viewModelOf(::DictionaryFormViewModel)

    includes(platformViewModels())
}

expect fun platformViewModels(): Module