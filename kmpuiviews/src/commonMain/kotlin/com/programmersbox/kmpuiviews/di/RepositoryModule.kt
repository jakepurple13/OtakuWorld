package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.customserver.CustomServerHandle
import com.programmersbox.kmpuiviews.domain.customserver.CustomServerHandler
import com.programmersbox.kmpuiviews.domain.customserver.FavoriteHandler
import com.programmersbox.kmpuiviews.domain.customserver.ListHandler
import com.programmersbox.kmpuiviews.domain.customserver.ServerRepository
import com.programmersbox.kmpuiviews.repository.ChangingSettingsRepository
import com.programmersbox.kmpuiviews.repository.CurrentSourceRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.repository.IncognitoRepository
import com.programmersbox.kmpuiviews.repository.ListRepository
import com.programmersbox.kmpuiviews.repository.PrereleaseRepository
import com.programmersbox.kmpuiviews.repository.SetupRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module

val repositories = module {
    singleOf(::SourceRepository)
    singleOf(::CurrentSourceRepository)
    singleOf(::ChangingSettingsRepository)
    singleOf(::FavoritesRepository)
    singleOf(::PrereleaseRepository)
    singleOf(::SetupRepository)
    includes(platformRepositories())
    singleOf(::ListRepository)
    singleOf(::ServerRepository)
    singleOf(::IncognitoRepository)
    //TODO: This will change into a repository that will return a CustomServerHandler based on data that changes
    single { CustomServerHandler(get(), get(), get()) } binds arrayOf(
        CustomServerHandle::class,
        FavoriteHandler::class,
        ListHandler::class
    )
}

expect fun platformRepositories(): Module