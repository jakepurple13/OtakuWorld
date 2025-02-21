package com.programmersbox.uiviews.di

import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.uiviews.presentation.settings.updateprerelease.PrereleaseRepository
import com.programmersbox.uiviews.repository.ChangingSettingsRepository
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
}