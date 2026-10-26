package com.programmersbox.desktop

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopMac
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
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.anime.shared.videochoice.VideoChoiceScreen
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.PlatformGenericInfo
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.domain.AppUpdate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class GenericAnimeDesktop(
    appConfig: AppConfig,
    private val navigationActions: NavigationActions,
    private val animeDownloadManager: AnimeDownloadManager,
    private val storageHolder: StorageHolder,
    private val trayState: TrayState,
) : GenericSharedAnime(appConfig = appConfig), PlatformGenericInfo {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Composable
    override fun DialogSetups() {
        VideoSourceModel.showVideoSources?.let {
            val navController = LocalNavActions.current
            VideoChoiceScreen(
                items = it.c,
                infoModel = it.infoModel,
                model = it.model,
                genericInfo = this,
                navController = navController,
                onPlay = { s ->
                    if (it.isStreaming) playResolved(navController, s, it.model, it.infoModel)
                    else downloadResolved(it.model, it.infoModel, s)
                }
            )
        }
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
    ) {
        storageHolder.storageModel = storage
        storageHolder.showTitle = infoModel.title
        storageHolder.posterUrl = infoModel.imageUrl
        navController.navigate(
            VideoScreen(
                showPath = storage.link.orEmpty(),
                showName = model.name,
                downloadOrStream = false,
                referer = storage.headers["referer"] ?: storage.source.orEmpty()
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
        downloadOnClick(
            model = model,
            infoModel = infoModel,
            onNone = {
                notifyUser("Download Failed", "${model.name}: no videos found", isError = true)
            },
            download = { downloadResolved(model, infoModel, it) },
        )
    }

    /** Lets the source resolve a lazily-described stream first, then hands it to the downloader. */
    suspend fun downloadResolved(model: KmpChapterModel, infoModel: KmpInfoModel, storage: KmpStorage) {
        val resolved = try {
            withContext(Dispatchers.IO) { model.source.resolve(storage) }.also { r ->
                if (r !== storage) {
                    if (r.headers.isEmpty()) r.headers.putAll(storage.headers)
                    if (r.extras.isEmpty()) r.extras.putAll(storage.extras)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            onResolveFailed(storage, e)
            return
        }
        if (resolved.link.isNullOrBlank()) {
            onResolveFailed(storage, IllegalStateException("No link"))
            return
        }
        enqueueDownload(resolved, model, infoModel)
    }

    /** The single hand-off to the downloader. */
    protected open fun enqueueDownload(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel) {
        // Interim: the manager re-fetches its own stream and ignores [storage]; Task 6 passes it through.
        animeDownloadManager.downloadChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoScreen> { VideoPlayerUi(it) }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<PlatformSettings> { JvmSettingsScreen() }
        navGraph.entry<VideoViewerRoute> { ViewVideoScreen() }
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
