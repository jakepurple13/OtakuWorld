package com.programmersbox.kmpuiviews.di

import androidx.compose.ui.window.TrayState
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.TranslationItemHandler
import com.programmersbox.kmpuiviews.TranslationModelHandlerImpl
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionShareHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.Zipper
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File

actual fun platformModule(): Module = module {
    singleOf(::DateTimeFormatHandler)
    singleOf(::IconLoader)
    singleOf(::PlatformDataStoreHandling)
    single { Zipper(getAll()) }
    singleOf(::ExtensionShareHandler)
    factoryOf(::ImageModifier)
    singleOf(::SystemAlerter)
    singleOf(::DownloadAndInstaller)
    singleOf(::BackgroundWorkHandlerImpl) { bind<BackgroundWorkHandler>() }
    factory<TranslationHandler> { TranslationItemHandler() }
    factory<TranslationModelHandler> { TranslationModelHandlerImpl() }
    singleOf(::TrayState)

    single {
        AppDirs {
            appName = get<AppConfig>().appName
            appAuthor = "jakepurple13"
        }
    }

    singleOf(::MangaDesktopSettings)

    single {
        SourceLoader(
            extensionsDir = { File(get<MangaDesktopSettings>().extensionDirectory.get()) },
            sourceType = get<KmpGenericInfo>().sourceType,
            sourceRepository = get(),
            appDirs = get(),
        )
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