package com.programmersbox.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.downloads.AnimeDownloadManager
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import dev.nucleusframework.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    val appDirs = AppDirs {
        appName = "AnimeWorld"
        appAuthor = "jakepurple13"
    }

    DataStoreSettings { File(appDirs.getUserDataDir(), it).absolutePath }

    if (BackgroundWorkHandlerImpl.setupSyncCheckers(args)) return
    val desktopViewModelStoreOwner = DesktopViewModelStoreOwner()
    application {
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides desktopViewModelStoreOwner
        ) {
            BaseDesktopUi(
                title = "AnimeWorld",
                moduleBlock = {
                    modules(
                        module {
                            single {
                                AppConfig(
                                    appName = "AnimeWorld",
                                    buildType = BuildType.NoFirebase,
                                    isDebug = false,
                                    userName = SystemInfo.users().firstOrNull()?.name
                                )
                            }
                            single { JvmAppLogo(Res.drawable.app_icon) }
                            singleOf(::GenericAnimeDesktop) { bindsGenericInfo() }
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
            )
        }
    }
}

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
