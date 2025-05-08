package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.viewModels
import com.programmersbox.uiviews.presentation.history.HistoryViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidViewModels = module {
    viewModelOf(::HistoryViewModel)
    viewModelOf(::AccountViewModel)
    includes(viewModels)
}