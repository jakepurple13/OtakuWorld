package com.programmersbox.desktop

import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling

class GenericMangaDesktop(
    val chapterHolder: ChapterHolder,
    settingsHandling: NewSettingsHandling,
    mangaSettingsHandling: MangaNewSettingsHandling,
    appConfig: AppConfig,
) : GenericSharedManga(
    settingsHandling = settingsHandling,
    mangaSettingsHandling = mangaSettingsHandling,
    appConfig = appConfig,
), PlatformGenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    override val sourceType: String get() = "manga"

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapters = allChapters
        chapterHolder.chapterModel = model
        ReadViewModel.navigateToMangaReader(
            navController,
            infoModel.title,
            model.url,
            model.sourceUrl
        )
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {

    }
}