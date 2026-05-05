package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling

class GenericMangaDesktop(
    val chapterHolder: ChapterHolder,
    settingsHandling: NewSettingsHandling,
    mangaSettingsHandling: MangaNewSettingsHandling,
    appConfig: AppConfig,
    navigationActions: NavigationActions,
) : GenericSharedManga(
    settingsHandling = settingsHandling,
    mangaSettingsHandling = mangaSettingsHandling,
    appConfig = appConfig,
    navigationActions = navigationActions,
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

    @Composable
    override fun ProfileIcon(): String = ""

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        super<GenericSharedManga>.settingsNav3Setup()
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit {
        val compose = ComposeSettingsDsl()
            .apply(super<GenericSharedManga>.composeCustomPreferences())

        return {
            viewSettings {
                compose.viewSettings(this)

                segmentedListItem(
                    content = { Text("Platform Settings") },
                    leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                    onClick = { navigationActions.navigate(PlatformSettings) }
                )
            }
            generalSettings = compose.generalSettings
            onboardingSettings = compose.onboardingSettings
            playerSettings = compose.playerSettings
        }
    }
}