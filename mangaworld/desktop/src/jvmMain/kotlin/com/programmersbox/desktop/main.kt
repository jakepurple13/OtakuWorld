package com.programmersbox.desktop

import androidx.compose.ui.window.application
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.bindsGenericInfo
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import org.koin.core.module.dsl.singleOf
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
