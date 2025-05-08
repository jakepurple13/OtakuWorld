package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import org.koin.dsl.module

val repository = module {
    single<DownloadStateInterface> { DownloadStateRepository(get(), get()) }
    includes(repositories)
}