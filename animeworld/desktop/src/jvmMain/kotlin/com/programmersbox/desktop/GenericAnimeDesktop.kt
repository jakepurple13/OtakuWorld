package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.EpisodeDownloadQueue
import com.programmersbox.anime.shared.episodeload.EpisodeLoadHost
import com.programmersbox.anime.shared.videoexport.VideoExportRoute
import com.programmersbox.anime.shared.videoexport.VideoExportScreen
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsScreen
import com.programmersbox.anime.shared.videoplayer.DesktopPlayerOnboarding
import com.programmersbox.anime.shared.videoplayer.DesktopPlayerSettings
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.FreeUpSpaceRoute
import com.programmersbox.anime.shared.videos.FreeUpSpaceScreen
import com.programmersbox.anime.shared.videos.LibraryFoldersRoute
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.domain.AppUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class GenericAnimeDesktop(
    appConfig: AppConfig,
    private val navigationActions: NavigationActions,
    private val episodeDownloads: EpisodeDownloadQueue,
    private val storageHolder: StorageHolder,
    private val trayState: TrayState,
) : GenericSharedAnime(appConfig = appConfig), PlatformGenericInfo {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Composable
    override fun DialogSetups() {
        EpisodeLoadHost(episodeLoader)
    }

    override fun onResolveFailed(storage: KmpStorage, error: Throwable) {
        val name = listOfNotNull(storage.source, storage.quality).joinToString(" ").ifBlank { "stream" }
        notifyUser("Playback Failed", "Couldn't load $name: ${error.message ?: error::class.simpleName}", isError = true)
    }

    /** Shows a tray notification; open so tests can capture messages instead. */
    protected open fun notifyUser(title: String, message: String, isError: Boolean) {
        scope.launch {
            withContext(Dispatchers.Main) {
                trayState.sendNotification(
                    Notification(
                        title = title,
                        message = message,
                        type = if (isError) Notification.Type.Error else Notification.Type.Info
                    )
                )
            }
        }
    }

    @Composable
    override fun ProfileIcon(): String = ""

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        seedColor: Int?,
    ) {
        storageHolder.storageModel = storage
        storageHolder.showTitle = infoModel.title
        storageHolder.posterUrl = infoModel.imageUrl
        navController.navigate(
            VideoScreen(
                showPath = storage.link.orEmpty(),
                showName = model.name,
                downloadOrStream = false,
                referer = storage.headers["referer"] ?: storage.source.orEmpty(),
                seedColor = seedColor
            )
        )
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        scope.launch { startDownload(model, infoModel) }
    }

    internal suspend fun startDownload(model: KmpChapterModel, infoModel: KmpInfoModel) {
        loadForDownload(model, infoModel)
    }

    override fun onNoDownloadSources(model: KmpChapterModel) {
        notifyUser("Download Failed", "${model.name}: no videos found", isError = true)
    }

    override suspend fun enqueueResolved(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel) {
        enqueueDownload(storage, model, infoModel)
    }

    /** The single hand-off to the downloader. */
    protected open fun enqueueDownload(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel) {
        episodeDownloads.download(
            storage = storage,
            referer = storage.headers["referer"] ?: storage.source,
            animeTitle = infoModel.title.ifBlank { infoModel.url },
            episodeName = model.name,
        )
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoScreen> { VideoPlayerUi(it) }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
        navGraph.entry<VideoViewerRoute> { ViewVideoScreen() }
        navGraph.entry<FreeUpSpaceRoute> { FreeUpSpaceScreen() }
        navGraph.entry<LibraryFoldersRoute> { LibraryFoldersScreen() }
        navGraph.entry<VideoExportRoute> { VideoExportScreen(it) }
        navGraph.entry<SubtitleSettingsRoute> { SubtitleSettingsScreen() }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            segmentedListItem(
                content = { Text("Videos") },
                leadingContent = { Icon(Icons.Default.VideoLibrary, null) },
                onClick = { navigationActions.navigate(VideoViewerRoute) }
            )
            segmentedListItem(
                content = { Text("Platform Settings") },
                leadingContent = { Icon(Icons.Default.DesktopMac, null) },
                onClick = { navigationActions.navigate(PlatformSettings) }
            )
        }

        playerSettings { DesktopPlayerSettings() }

        onboardingSettings {
            item { DesktopPlayerOnboarding() }
        }
    }
}
