package com.programmersbox.desktop

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.desktopSetup
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.JvmAppLogo
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
import org.koin.dsl.navigation3.navigation
import java.io.File

fun main(args: Array<String>) {
    desktopSetup(
        args = args,
        name = "MangaWorld",
        appConfig = {
            AppConfig(
                appName = "MangaWorld",
                buildType = BuildType.NoFirebase,
                isDebug = false,
                userName = SystemInfo.users().firstOrNull()?.name
            )
        },
        jvmAppLogo = { JvmAppLogo(Res.drawable.app_icon) },
        genericInfo = { singleOf(::GenericMangaDesktop) },
        appDirs = AppDirs {
            appName = "MangaWorld"
            appAuthor = "jakepurple13"
        },
        moduleBlock = {
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

private class DesktopViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}