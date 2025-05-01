package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import com.programmersbox.uiviews.presentation.settings.updateprerelease.PrereleaseRepository
import com.programmersbox.uiviews.repository.NotificationRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf

fun Module.repository() {
    singleOf(::NotificationRepository)
    singleOf(::PrereleaseRepository)
    singleOf(::DownloadStateRepository)
    includes(repositories)
}