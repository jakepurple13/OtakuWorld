package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.kmpuiviews.repository.NotificationRepository
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf

fun Module.repository() {
    singleOf(::NotificationRepository)
    singleOf(::DownloadStateRepository)
    includes(repositories)
}