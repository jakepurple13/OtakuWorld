package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.UrlOpenerViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun platformViewModels(): Module = module {
    viewModelOf(::UrlOpenerViewModel)
}