package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.screensaver.CoverCarouselViewModel
import com.programmersbox.kmpuiviews.screensaver.CustomListRotationViewModel
import com.programmersbox.kmpuiviews.screensaver.HistoryFeedViewModel
import com.programmersbox.kmpuiviews.screensaver.ScreensaverViewModel
import com.programmersbox.kmpuiviews.screensaver.StatsDashboardViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

actual fun platformViewModels(): Module = module {
    viewModelOf(::ScreensaverViewModel)
    viewModelOf(::CoverCarouselViewModel)
    viewModelOf(::HistoryFeedViewModel)
    viewModelOf(::StatsDashboardViewModel)
    viewModelOf(::CustomListRotationViewModel)
}