package com.programmersbox.kmpuiviews.di

import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionShareHandler
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.Zipper
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    singleOf(::DateTimeFormatHandler)
    singleOf(::IconLoader)
    singleOf(::PlatformDataStoreHandling)
    singleOf(::Zipper)
    singleOf(::ExtensionShareHandler)
    factoryOf(::ImageModifier)
    singleOf(::SystemAlerter)
    singleOf(::DownloadAndInstaller)

    single {
        AppDirs {
            appName = get<AppConfig>().appName
            appAuthor = "jakepurple13"
        }
    }

    single {
        NewSettingsHandling(
            createProtobuf(
                serializer = SettingsSerializer(),
                fileName = File(
                    get<AppDirs>().getUserDataDir(),
                    "Settings.preferences_pb"
                ).absolutePath
            ),
        )
    }
}