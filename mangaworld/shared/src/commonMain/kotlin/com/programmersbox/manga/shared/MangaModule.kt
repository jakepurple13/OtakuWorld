package com.programmersbox.manga.shared

import com.programmersbox.kmpuiviews.di.backupProcessor
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.reader.ReadViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun mangaSharedModule() = module {
    backupProcessor("manga_settings", ::MangaNewSettingsBackupProcessor)
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
    viewModelOf(::DownloadViewModel)
}