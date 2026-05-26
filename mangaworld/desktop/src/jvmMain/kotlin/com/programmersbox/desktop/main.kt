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
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import io.github.kdroidfilter.nucleus.systeminfo.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
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
                            singleOf(::GenericMangaDesktop) { bindsGenericInfo() }
                            singleOf(::ChapterHolder)
                            factoryOf(::DownloadedMediaHandler)
                            single {
                                ExtensionWatcher(
                                    extensionsDir = get<MangaDesktopSettings>()
                                        .extensionDirectory
                                        .asFlow()
                                )
                            }
                            viewModelOf(::ReadViewModel)
                            viewModelOf(::DownloadViewModel)
                            single { MangaDownloadManager(CoroutineScope(Dispatchers.IO + SupervisorJob())) }
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