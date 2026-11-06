package com.programmersbox.kmpuiviews.di

import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.ui.DesktopTranslationScreen
import com.programmersbox.kmpuiviews.translation.ui.DesktopTranslationViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
actual fun platformViewModels(): Module = module {
    viewModel {
        DesktopTranslationViewModel(
            settings = get(),
            appSettings = get(),
            engines = getAll<TranslationEngine>().associateBy { it.kind },
            store = get(),
            runtime = get(),
            ollama = get(),
            server = get(),
            detector = get(),
            notifier = get(),
        )
    }
    navigation<Screen.Settings.Translation> { DesktopTranslationScreen() }
}
