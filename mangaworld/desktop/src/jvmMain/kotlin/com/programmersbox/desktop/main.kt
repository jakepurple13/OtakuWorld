package com.programmersbox.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.manga.shared.mangaSharedModule
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import dev.nucleusframework.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    val appDirs = AppDirs {
        appName = "MangaWorld"
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
                title = "MangaWorld",
                moduleBlock = {
                    //monitoring()
                    modules(
                        module {
                            single {
                                AppConfig(
                                    appName = "MangaWorld",
                                    buildType = BuildType.NoFirebase,
                                    isDebug = false,
                                    userName = SystemInfo.users().firstOrNull()?.name
                                )
                            }
                            single { JvmAppLogo(Res.drawable.app_icon) }
                            singleOf(::GenericMangaDesktop) { bindsGenericInfo() }
                            factoryOf(::DownloadedMediaHandler)
                            single {
                                ExtensionWatcher(
                                    extensionsDir = get<MangaDesktopSettings>()
                                        .extensionDirectory
                                        .asFlow()
                                )
                            }
                            single {
                                MangaDownloadManager(
                                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                                    mangaDesktopSettings = get(),
                                    trayState = get(),
                                )
                            }
                            single {
                                MangaNewSettingsHandling(
                                    createProtobuf(
                                        serializer = MangaNewSettingsSerializer,
                                        fileName = File(
                                            get<AppDirs>().getUserDataDir(),
                                            "MangaSettings.preferences_pb"
                                        ).absolutePath
                                    )
                                )
                            }

                            navigation<PlatformSettings> { JvmSettingsScreen() }

                            includes(mangaSharedModule())
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