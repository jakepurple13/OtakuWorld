package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.FavoritesSearchRepository
import com.programmersbox.kmpuiviews.repository.IncognitoRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.repository.PrereleaseRepository
import com.programmersbox.kmpuiviews.repository.SetupRepository
import com.programmersbox.sharedcomponents.repository.ChangingSettingsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositories = module {
    singleOf(::SourceRepository)
    singleOf(::CurrentSourceRepository)
    singleOf(::ChangingSettingsRepository)
    // Explicit so the defaulted clock stays Clock.System; singleOf would ask Koin for a Clock
    single { FavoritesRepository(dao = get(), systemAlerter = get(), authManager = get()) }
    singleOf(::FavoritesSearchRepository)
    singleOf(::PrereleaseRepository)
    singleOf(::SetupRepository)
    includes(platformRepositories())
    singleOf(::ListRepository)
    singleOf(::IncognitoRepository)
    singleOf(::BookmarkRepository)
}

expect fun platformRepositories(): Module