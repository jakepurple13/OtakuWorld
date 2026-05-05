package com.programmersbox.desktop

import androidx.compose.ui.window.application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ExtensionWatcher
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File

fun main() {
    DataStoreSettings { File(System.getProperty("user.home"), it).absolutePath }
    application {
        BaseDesktopUi(
            title = "MangaWorld",
            moduleBlock = {
                modules(
                    module {
                        single {
                            AppConfig(
                                "MangaWorld",
                                BuildType.NoFirebase,
                                false
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
