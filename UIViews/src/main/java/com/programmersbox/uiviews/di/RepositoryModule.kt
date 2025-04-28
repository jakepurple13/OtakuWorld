package com.programmersbox.uiviews.di

import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import com.programmersbox.uiviews.presentation.settings.updateprerelease.PrereleaseRepository
import com.programmersbox.uiviews.repository.CurrentSourceRepository
import com.programmersbox.uiviews.repository.FavoritesRepository
import com.programmersbox.uiviews.repository.NotificationRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf

fun Module.repository() {
    singleOf(::SourceRepository)
    singleOf(::CurrentSourceRepository)
    singleOf(::ChangingSettingsRepository)
    singleOf(::NotificationRepository)
    singleOf(::FavoritesRepository)
    singleOf(::PrereleaseRepository)
    singleOf(::DownloadStateRepository)
}