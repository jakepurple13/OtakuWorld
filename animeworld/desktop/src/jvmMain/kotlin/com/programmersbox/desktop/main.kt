package com.programmersbox.desktop

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.subtitles.JvmSubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsViewModel
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.anime.shared.downloads.DownloadQueueSource
import com.programmersbox.anime.shared.downloads.EpisodeDownloadQueue
import com.programmersbox.anime.shared.videoexport.VideoExportManager
import com.programmersbox.anime.shared.videoexport.VideoExportRoute
import com.programmersbox.anime.shared.videoexport.VideoExportScreen
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.aniyomi.aniyomiModule
import com.programmersbox.cloudstream.cloudstreamModule
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.desktopSetup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import dev.nucleusframework.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    desktopSetup(
        args = args,
        name = "AnimeWorld",
        appConfig = {
            AppConfig(
                appName = "AnimeWorld",
                buildType = BuildType.NoFirebase,
                isDebug = false,
                userName = SystemInfo.users().firstOrNull()?.name
            )
        },
        appDirs = AppDirs {
            appName = "AnimeWorld"
            appAuthor = "jakepurple13"
        },
        jvmAppLogo = { JvmAppLogo(Res.drawable.app_icon) },
        genericInfo = { singleOf(::GenericAnimeDesktop) },
        moduleBlock = {
            singleOf(::AnimeDesktopSettings)
            singleOf(::StorageHolder)
            single<SubtitleSettingsRepository> {
                JvmSubtitleSettingsRepository(File(get<AppDirs>().getUserDataDir(), "subtitle_style.json"))
            }
            viewModelOf(::SubtitleSettingsViewModel)
            singleOf(::VideoLibrarySource)
            singleOf(::DownloadQueueSource)
            single {
                ExtensionWatcher(
                    extensionsDir = get<MangaDesktopSettings>()
                        .extensionDirectory
                        .asFlow()
                )
            }
            single {
                AnimeDownloadManager(
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    animeDesktopSettings = get(),
                    trayState = get(),
                )
            } bind EpisodeDownloadQueue::class
            single {
                VideoExportManager(
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    settings = get(),
                    trayState = get(),
                )
            }

            navigation<VideoScreen> { VideoPlayerUi(it) }
            navigation<PlatformSettings> { JvmSettingsScreen() }
            navigation<VideoViewerRoute> { ViewVideoScreen() }
            navigation<VideoExportRoute> { VideoExportScreen(it) }

            includes(aniyomiModule, cloudstreamModule)
        }
    )
}
