package com.programmersbox.koogintegration.customscraper.di

import com.programmersbox.koogintegration.customscraper.database.ScraperDatabase
import com.programmersbox.koogintegration.customscraper.presentation.CustomScraperViewModel
import com.programmersbox.koogintegration.customscraper.repository.CustomScraperRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun scraperModule() = module {
    singleOf(::CustomScraperRepository)
    single { ScraperDatabase.getInstance(get()) }
    viewModelOf(::CustomScraperViewModel)
    includes(platformModule())
}

expect fun platformModule(): Module