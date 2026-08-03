package com.programmersbox.desktop

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
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
import org.koin.dsl.navigation3.navigation

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
            singleOf(::VideoLibrarySource)
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
            }

            navigation<VideoScreen> { VideoPlayerUi(it) }
            navigation<PlatformSettings> { JvmSettingsScreen() }
            navigation<VideoViewerRoute> { ViewVideoScreen() }
        }
    )
}
