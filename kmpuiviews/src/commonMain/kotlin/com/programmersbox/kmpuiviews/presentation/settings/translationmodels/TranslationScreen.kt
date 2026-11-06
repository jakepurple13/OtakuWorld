package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val state by vm.state.collectAsStateWithLifecycle()

    // Minimal placeholder until the sheet UI is rewritten.
    ListBottomScreen(
        title = stringResource(Res.string.chooseModelToDelete),
        list = state.downloaded.filter { !it.required },
        onClick = { item ->
            vm.askDelete(item.language.code)
            vm.confirmDelete()
        },
    ) {
        ListBottomSheetItemModel(
            primaryText = it.language.englishName,
            overlineText = it.language.nativeName,
            icon = Icons.Default.Delete
        )
    }
}
