package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    singleOf(::DownloadAndInstaller)
}