package com.programmersbox.desktop

import androidx.compose.ui.window.application
import com.programmersbox.kmpuiviews.BaseDesktopUi
import org.koin.dsl.module

fun main() = application {
    BaseDesktopUi(
        title = "MangaWorld",
        moduleBlock = {
            modules(
                module {
                    single { GenericMangaDesktop(get()) }
                }
            )
        }
    )
}
