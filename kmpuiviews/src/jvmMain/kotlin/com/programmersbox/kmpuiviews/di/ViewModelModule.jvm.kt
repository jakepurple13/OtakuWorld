package com.programmersbox.kmpuiviews.di

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.ui.DesktopTranslationScreen
import com.programmersbox.kmpuiviews.translation.ui.DesktopTranslationViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3AdaptiveApi::class)
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
    // A settings detail pane, like the other Screen.Settings.* entries (Nav3Graph's detailEntry).
    navigation<Screen.Settings.Translation>(metadata = ListDetailSceneStrategy.detailPane()) { DesktopTranslationScreen() }
}
