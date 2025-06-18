package com.programmersbox.desktop

import androidx.compose.ui.window.application
import com.programmersbox.datastore.DataStoreSettings
import com.programmersbox.kmpuiviews.BaseDesktopUi
import com.programmersbox.kmpuiviews.KmpGenericInfo
import org.koin.core.module.dsl.binds
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
                        singleOf(::GenericMangaDesktop) {
                            binds(
                                listOf(
                                    KmpGenericInfo::class,
                                )
                            )
                        }
                    }
                )
            }
        )
    }
}
