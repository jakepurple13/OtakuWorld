package com.programmersbox.kmpuiviews.di

import android.content.Context
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
import com.programmersbox.kmpuiviews.dailyoverview.GemmaEngine
import com.programmersbox.kmpuiviews.dailyoverview.NanoEngine
import com.programmersbox.kmpuiviews.dailyoverview.ProviderEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewScheduler
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.presentation.settings.extensions.ExtensionShareHandler
import com.programmersbox.kmpuiviews.presentation.updatecheck.AndroidUpdateCheckReportSharer
import com.programmersbox.kmpuiviews.presentation.updatecheck.UpdateCheckReportSharer
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
import com.programmersbox.kmpuiviews.repository.PlatformRepository
import com.programmersbox.kmpuiviews.utils.DeepLinks
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.PackageInstallEngine
import com.programmersbox.kmpuiviews.workers.WorkManagerDailyOverviewScheduler
import com.programmersbox.kmpuiviews.workers.UpdateNotification
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    singleOf(::PackageInstallEngine)
    singleOf(::InstallStatusRepository)
    singleOf(::DownloadAndInstaller)
    singleOf(::IconLoader)
    singleOf(::DateTimeFormatHandler)
    singleOf(::AboutLibraryBuilder)
    singleOf(::PlatformDataStoreHandling)
    single {
        NewSettingsHandling(
            createProtobuf(
                context = get<Context>(),
                serializer = SettingsSerializer()
            ),
        )
    }

    single {
        SourceLoader(
            application = get(),
            context = get(),
            sourceType = get<KmpGenericInfo>().sourceType,
            sourceRepository = get(),
            nativeCatalogs = get(),
        )
    }

    singleOf(::DeepLinks)

    singleOf(::UpdateNotification)
    singleOf(::PlatformRepository)
    singleOf(::ExtensionShareHandler)
    singleOf(::AndroidUpdateCheckReportSharer) { bind<UpdateCheckReportSharer>() }
    factoryOf(::ImageModifier)
    singleOf(::EncryptedDataStoreFactory)
    singleOf(::NanoEngine) { bind<SummaryEngine>() }
    singleOf(::GemmaEngine) { bind<SummaryEngine>() }
    singleOf(::ProviderEngine) { bind<SummaryEngine>() }
    single<DailyOverviewScheduler> { WorkManagerDailyOverviewScheduler(get()) }
    kmpWorkers()
    includes(jsExtensionLoaderModule)
}