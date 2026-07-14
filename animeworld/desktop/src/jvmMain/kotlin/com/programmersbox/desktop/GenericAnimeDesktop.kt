package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.VideoNotSupportedRoute
import com.programmersbox.anime.shared.VideoNotSupportedScreen
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl

class GenericAnimeDesktop(
    appConfig: AppConfig,
    private val navigationActions: NavigationActions,
    private val animeDownloadManager: AnimeDownloadManager,
) : GenericSharedAnime(appConfig = appConfig), PlatformGenericInfo {

    @Composable
    override fun ProfileIcon(): String = ""

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        navController.navigate(VideoNotSupportedRoute)
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        animeDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoNotSupportedRoute> { VideoNotSupportedScreen() }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            segmentedListItem(
                content = { Text("Platform Settings") },
                leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                onClick = { navigationActions.navigate(PlatformSettings) }
            )
        }
    }
}
