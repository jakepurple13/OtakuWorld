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
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformRepositories(): Module = module {
    singleOf(::NotificationRepository)
    singleOf(::WorkerRepositoryImpl) { bind<WorkRepository>() }
    singleOf(::DownloadStateRepository) { bind<DownloadStateInterface>() }
    singleOf(::NotificationScreenRepository) { bind<NotificationScreenInterface>() }
    singleOf(::SourceInfoRepository)
    singleOf(::PlatformRepository)
}