package com.programmersbox.kmpuiviews.di

import androidx.compose.ui.window.TrayState
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.PlatformDataStoreHandling
import com.programmersbox.datastore.SettingsSerializer
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.datastore.encrypted.EncryptedDataStoreFactory
import com.programmersbox.jsextensionloader.jsExtensionLoaderModule
import com.programmersbox.kmpextensionloader.SourceLoader
import com.programmersbox.kmpuiviews.AboutLibraryBuilder
import com.programmersbox.kmpuiviews.DateTimeFormatHandler
import com.programmersbox.kmpuiviews.IconLoader
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.MangaDesktopSettings
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.translation.DesktopTranslationHandler
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.LanguageDetector
import com.programmersbox.kmpuiviews.translation.LibraryLanguageDetector
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotCliRuntime
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotEngine
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotIndex
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelStore
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRuntime
import com.programmersbox.kmpuiviews.translation.ollama.OllamaClient
import com.programmersbox.kmpuiviews.translation.ollama.OllamaEngine
import com.programmersbox.kmpuiviews.translation.ollama.installOllamaTimeouts
import com.programmersbox.kmpuiviews.TranslationModelHandlerImpl
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionShareHandler
import com.programmersbox.kmpuiviews.presentation.updatecheck.ClipboardUpdateCheckReportSharer
import com.programmersbox.kmpuiviews.presentation.updatecheck.UpdateCheckReportSharer
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.repository.BackupResultsHolder
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.Zipper
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.qualifier.named
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
    singleOf(::ClipboardUpdateCheckReportSharer) { bind<UpdateCheckReportSharer>() }
    factoryOf(::ImageModifier)
    singleOf(::SystemAlerter)
    singleOf(::DownloadAndInstaller)
    singleOf(::BackupResultsHolder)
    singleOf(::BackgroundWorkHandlerImpl) { bind<BackgroundWorkHandler>() }
    single { DesktopTranslationSettings(get()) }
    single<LanguageDetector> { LibraryLanguageDetector() }
    single { OllamaClient(HttpClient { installOllamaTimeouts() }, get()) }
    // Later engines register their own single<TranslationEngine> (distinct qualifier); handler maps them by kind.
    single<TranslationEngine>(named("ollama")) { OllamaEngine(get(), get()) }
    single {
        val modelsDir = File(get<AppDirs>().getUserDataDir(), "translation/bergamot")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val client = HttpClient()
        BergamotModelStore(
            index = BergamotIndex(client, cacheFile = File(modelsDir, "index.json")),
            dir = modelsDir,
            client = client,
            scope = scope,
        ).also { store ->
            val appSettings = get<NewSettingsHandling>()
            scope.launch { appSettings.translationTargetLanguage.asFlow().collect(store::setTarget) }
            scope.launch { store.refresh() }
        }
    }
    single<BergamotRuntime> {
        BergamotCliRuntime(installRoot = File(get<AppDirs>().getUserDataDir(), "translation/bergamot-runtime"))
    }
    single<TranslationEngine>(named("bergamot")) { BergamotEngine(get(), get()) }
    factory<TranslationHandler> {
        DesktopTranslationHandler(
            engines = getAll<TranslationEngine>().associateBy { it.kind },
            detector = get(),
            settings = get(),
            appSettings = get(),
            notifier = get(),
        )
    }
    factory<TranslationModelHandler> { TranslationModelHandlerImpl() }
    singleOf(::TrayState)
    singleOf(::AboutLibraryBuilder)

    single {
        AppDirs {
            appName = get<AppConfig>().appName
            appAuthor = "jakepurple13"
        }
    }

    singleOf(::MangaDesktopSettings)
    single { EncryptedDataStoreFactory(get<AppDirs>().getUserDataDir()) }

    single {
        SourceLoader(
            extensionsDir = { File(get<MangaDesktopSettings>().extensionDirectory.get()) },
            sourceType = get<KmpGenericInfo>().sourceType,
            sourceRepository = get(),
            appDirs = get(),
            nativeCatalogs = get(),
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

    includes(jsExtensionLoaderModule)
}