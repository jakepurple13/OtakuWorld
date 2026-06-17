package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.HideNavBarWhileOnScreen
import com.programmersbox.koogintegration.screens.chatscreen.ChatScreen
import com.programmersbox.koogintegration.screens.chatscreen.KoogNavigation
import com.programmersbox.koogintegration.screens.settings.KoogSettingsScreen
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.GenericSharedManga
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class GenericMangaDesktop(
    val chapterHolder: ChapterHolder,
    settingsHandling: NewSettingsHandling,
    mangaSettingsHandling: MangaNewSettingsHandling,
    appConfig: AppConfig,
    navigationActions: NavigationActions,
    private val desktopSettings: MangaDesktopSettings,
    mangaDownloadManager: MangaDownloadManager,
) : GenericSharedManga(
    settingsHandling = settingsHandling,
    mangaSettingsHandling = mangaSettingsHandling,
    appConfig = appConfig,
    navigationActions = navigationActions,
    mangaDownloadManager = mangaDownloadManager,
), PlatformGenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    override val sourceType: String get() = "manga"

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        if (desktopSettings.useWebViewForReader.get()) {
            navigationActions.webView(model.url)
        } else {
            chapterHolder.chapters = allChapters
            chapterHolder.chapterModel = model
            ReadViewModel.navigateToMangaReader(
                navController,
                infoModel.title,
                model.url,
                model.sourceUrl
            )
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        mangaDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    @Composable
    override fun ProfileIcon(): String = ""

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        super<GenericSharedManga>.settingsNav3Setup()
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
        navGraph.entry<KoogSettings> {
            KoogSettingsScreen(
                onBack = { navigationActions.popBackStack() }
            )
        }
        navGraph.entry<Koog> {
            HideNavBarWhileOnScreen()
            ChatScreen(
                viewModel = koinViewModel { parametersOf("otaku_agent") },
                koogNavigation = KoogNavigation(
                    onBack = { navigationActions.popBackStack() },
                    onKoogSettingsClick = { navigationActions.navigate(KoogSettings) },
                    onSearchClick = { navigationActions.globalSearch(it) },
                    onListClick = { navigationActions.customList() }
                )
            )
        }

        /*navGraph.entry<CustomScraper> {
            CustomScraperScreen(
                onBack = { navigationActions.popBackStack() }
            )
        }*/
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

                segmentedListItem(
                    content = { Text("Koog") },
                    leadingContent = { Icon(Icons.Default.Bolt, null) },
                    onClick = { navigationActions.navigate(Koog) }
                )

                /*segmentedListItem(
                    content = { Text("Custom Scraper") },
                    leadingContent = { Icon(Icons.Default.Bolt, null) },
                    onClick = { navigationActions.navigate(CustomScraper) }
                )*/
            }
            generalSettings = compose.generalSettings
            onboardingSettings = compose.onboardingSettings
            playerSettings = compose.playerSettings
        }
    }
}

@Serializable
data object KoogSettings : NavKey

@Serializable
data object Koog : NavKey

@Serializable
data object CustomScraper : NavKey