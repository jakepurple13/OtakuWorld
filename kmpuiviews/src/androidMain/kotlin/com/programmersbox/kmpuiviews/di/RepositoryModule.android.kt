package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.notifications.NotificationScreenInterface
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandler
import com.programmersbox.kmpuiviews.repository.BackgroundWorkHandlerImpl
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.repository.DownloadStateRepository
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.NotificationScreenRepository
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import com.programmersbox.kmpuiviews.repository.SourceInfoRepository
import com.programmersbox.kmpuiviews.repository.WorkRepository
import com.programmersbox.kmpuiviews.repository.WorkRepositoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformRepositories(): Module = module {
    singleOf(::NotificationRepository)
    singleOf(::QrCodeRepository)
    singleOf(::SourceInfoRepository)
    singleOf(::NotificationScreenRepository) { bind<NotificationScreenInterface>() }
    singleOf(::WorkRepositoryImpl) { bind<WorkRepository>() }
    singleOf(::BackgroundWorkHandlerImpl) { bind<BackgroundWorkHandler>() }
    singleOf(::DownloadStateRepository) { bind<DownloadStateInterface>() }
}