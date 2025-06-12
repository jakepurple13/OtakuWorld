package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.uiviews.di.kmpinterop.DownloadStateRepository
import org.koin.dsl.module

val repository = module {
    single<DownloadStateInterface> { DownloadStateRepository(get(), get()) }
}