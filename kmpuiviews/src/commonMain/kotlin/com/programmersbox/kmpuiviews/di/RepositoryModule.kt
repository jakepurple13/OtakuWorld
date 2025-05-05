package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.PrereleaseRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositories = module {
    singleOf(::SourceRepository)
    singleOf(::CurrentSourceRepository)
    singleOf(::ChangingSettingsRepository)
    singleOf(::FavoritesRepository)
    singleOf(::PrereleaseRepository)
}