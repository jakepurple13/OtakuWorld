package com.programmersbox.uiviews.di

import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidViewModels = module {
    viewModelOf(::AccountViewModel)
}