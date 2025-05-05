package com.programmersbox.desktop

import androidx.compose.ui.window.application
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.BaseDesktopUi
import org.koin.compose.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun main() = application {
    KoinApplication(
        application = {
            //TODO: Also need to create a generic module in kmpuiviews
            modules(
                module {
                    singleOf(::DataStoreHandling)
                    single {
                        NewSettingsHandling(
                            createProtobuf(
                                serializer = SettingsSerializer()
                            ),
                        )
                    }

                    single { GenericMangaDesktop(get()) }
                }
            )
        }
    ) {
        BaseDesktopUi("MangaWorld")
    }
}
