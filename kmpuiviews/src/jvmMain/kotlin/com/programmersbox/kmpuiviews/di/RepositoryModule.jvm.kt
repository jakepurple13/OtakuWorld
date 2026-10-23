package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.DownloadStateRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.NotificationScreenRepository
import com.programmersbox.kmpuiviews.repository.PlatformRepository
import com.programmersbox.kmpuiviews.repository.SourceInfoRepository
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.kmpuiviews.repository.WorkerRepositoryImpl
import ca.gosyer.appdirs.AppDirs
import com.programmersbox.kmpextensionloader.SourceLoader
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

actual fun platformRepositories(): Module = module {
    singleOf(::NotificationRepository)
    singleOf(::WorkerRepositoryImpl) { bind<WorkRepository>() }
    single {
        DownloadStateRepository(
            client = HttpClient(),
            stores = getAll(),
            tempDir = File(get<AppDirs>().getUserCacheDir(), "extension-downloads"),
            reload = { get<SourceLoader>().blockingLoad() },
        )
    } bind DownloadStateInterface::class
    singleOf(::NotificationScreenRepository) { bind<NotificationScreenInterface>() }
    single { SourceInfoRepository(getAll()) }
    singleOf(::PlatformRepository)
}