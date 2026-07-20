package com.programmersbox.manga.shared

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import com.programmersbox.kmpuiviews.di.backupProcessorWithUiInfo
import com.programmersbox.manga.shared.downloads.DownloadRoute
import com.programmersbox.manga.shared.downloads.DownloadScreen
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.reader.ReadView
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.manga.shared.settings.ImageLoaderSettingsRoute
import com.programmersbox.manga.shared.settings.ReaderSettings
import com.programmersbox.manga.shared.settings.ReaderSettingsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
fun mangaSharedModule() = module {
    backupProcessorWithUiInfo("manga_settings", ::MangaNewSettingsBackupProcessor)
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
    viewModelOf(::DownloadViewModel)

    navigation<ReadViewModel.MangaReader> {
        ReadView(
            viewModel = koinViewModel { parametersOf(it) }
        )
    }

    navigation<DownloadRoute> {
        DownloadScreen()
    }

    navigation<ImageLoaderSettingsRoute> {
        ImageLoaderSettings(koinInject())
    }

    navigation<ReaderSettingsScreen> {
        ReaderSettings(
            mangaSettingsHandling = koinInject(),
            settingsHandling = koinInject()
        )
    }
}