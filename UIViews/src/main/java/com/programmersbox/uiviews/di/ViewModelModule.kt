package com.programmersbox.uiviews.di

import com.programmersbox.kmpuiviews.di.viewModels
import com.programmersbox.uiviews.presentation.history.HistoryViewModel
import com.programmersbox.uiviews.presentation.settings.viewmodels.AccountViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf

fun Module.viewModels() {
    viewModelOf(::HistoryViewModel)
    viewModelOf(::AccountViewModel)
    includes(viewModels)
}