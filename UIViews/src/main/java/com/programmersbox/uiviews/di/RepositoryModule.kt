package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.repositories
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.uiviews.presentation.settings.downloadstate.DownloadStateRepository
import org.koin.core.module.Module

fun Module.repository() {
    single<DownloadStateInterface> { DownloadStateRepository(get(), get()) }
    includes(repositories)
}