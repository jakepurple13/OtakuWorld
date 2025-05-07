package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.chooseModelToDelete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun showTranslationScreen(): MutableState<Boolean> {
    val showTranslationScreen = remember { mutableStateOf(false) }

    if (showTranslationScreen.value) {
        ModalBottomSheet(
            onDismissRequest = { showTranslationScreen.value = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            TranslationScreen()
        }
    }

    return showTranslationScreen
}

@Composable
fun TranslationScreen(vm: TranslationViewModel = koinViewModel()) {
    LifecycleResumeEffect(Unit) {
        vm.loadModels()
        onPauseOrDispose {}
    }

    ListBottomScreen(
        title = stringResource(Res.string.chooseModelToDelete),
        list = vm.translationModels.toList(),
        onClick = { item -> vm.deleteModel(item) },
    ) {
        ListBottomSheetItemModel(
            primaryText = it.language,
            overlineText = try {
                Locale(it.language).region
            } catch (_: Exception) {
                null
            },
            icon = Icons.Default.Delete
        )
    }
}