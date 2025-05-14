package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.kmpuiviews.repository.QrCodeRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformRepositories(): Module = module {
    singleOf(::NotificationRepository)
    singleOf(::QrCodeRepository)
}