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
import com.programmersbox.kmpuiviews.dailyoverview.OllamaEngine as OllamaSummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewScheduler
import com.programmersbox.kmpuiviews.domain.dailyoverview.NoOpDailyOverviewScheduler
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.translation.DesktopTranslationHandler
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.LanguageDetector
import com.programmersbox.kmpuiviews.translation.LibraryLanguageDetector
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.custom.CustomEngine
import com.programmersbox.kmpuiviews.translation.custom.OpenAiCompatClient
import com.programmersbox.kmpuiviews.translation.custom.installOpenAiCompatTimeouts
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
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import com.programmersbox.kmpuiviews.dailyoverview.LocalModelEngine
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.LocalModel
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ModelImporter
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ModelImports
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.PlatformModelImports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import java.io.File
import com.programmersbox.kmpuiviews.presentation.debug.navmap.FileNavGraphSource
import com.programmersbox.kmpuiviews.presentation.debug.navmap.NavGraphSource

actual fun platformModule(): Module = module {
    // Local `run` tasks pass the generated graph's path; packaged builds have none.
    single<NavGraphSource> { FileNavGraphSource() }
    singleOf(::DateTimeFormatHandler)
    singleOf(::IconLoader)
    singleOf(::PlatformDataStoreHandling)
    single { Zipper(getAll()) }
    singleOf(::ExtensionShareHandler)
    singleOf(::ClipboardUpdateCheckReportSharer) { bind<UpdateCheckReportSharer>() }
    factoryOf(::ImageModifier)
    singleOf(::SystemAlerter)
    single<DailyOverviewScheduler> { NoOpDailyOverviewScheduler }
    singleOf(::DownloadAndInstaller)
    singleOf(::BackupResultsHolder)
    singleOf(::BackgroundWorkHandlerImpl) { bind<BackgroundWorkHandler>() }
    single { DesktopTranslationSettings() }
    single<LanguageDetector> { LibraryLanguageDetector() }
    single { OllamaClient(HttpClient { installOllamaTimeouts() }, get()) }
    // Later engines register their own single<TranslationEngine> (distinct qualifier); handler maps them by kind.
    single<TranslationEngine>(named("ollama")) { OllamaEngine(get(), get()) }
    single { OpenAiCompatClient(HttpClient { installOpenAiCompatTimeouts() }, get()) }
    single<TranslationEngine>(named("custom")) { CustomEngine(get(), get()) }
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
    single { OllamaSummaryEngine(get(), HttpClient()) } bind SummaryEngine::class
    // Same folder AI Helper's ModelManager uses on desktop
    single {
        val dataDir = File(get<AppDirs>().getUserDataDir())
        LocalModelEngine(File(dataDir, "models"), File(dataDir, "models-cache"))
    } binds arrayOf(SummaryEngine::class, LocalModel::class)
    single {
        val dataDir = File(get<AppDirs>().getUserDataDir())
        ModelImporter(
            modelsDir = File(dataDir, "models"),
            cacheDir = File(dataDir, "models-cache"),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            verify = get<LocalModel>()::verify,
            analytics = get(),
        )
    }
    single<ModelImports> { PlatformModelImports(get(), get()) }
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