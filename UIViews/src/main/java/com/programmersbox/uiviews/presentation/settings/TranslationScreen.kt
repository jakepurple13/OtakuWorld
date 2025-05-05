package com.programmersbox.uiviews.presentation.settings

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.kmpuiviews.presentation.components.ListBottomScreen
import com.programmersbox.kmpuiviews.presentation.components.ListBottomSheetItemModel
import com.programmersbox.sharedutils.CustomRemoteModel
import com.programmersbox.sharedutils.TranslatorUtils
import com.programmersbox.uiviews.R
import kotlinx.coroutines.launch
import java.util.Locale

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

class TranslationViewModel : ViewModel() {

    var translationModels: List<CustomRemoteModel> by mutableStateOf(emptyList())
        private set

    fun loadModels() {
        viewModelScope.launch {
            translationModels = TranslatorUtils.modelList()
        }
    }

    fun deleteModel(model: CustomRemoteModel) {
        viewModelScope.launch {
            TranslatorUtils.delete(model)
            translationModels = TranslatorUtils.modelList()
        }
    }

}

@Composable
fun TranslationScreen(vm: TranslationViewModel = viewModel()) {
    LifecycleResumeEffect(Unit) {
        vm.loadModels()
        onPauseOrDispose {}
    }

    ListBottomScreen(
        title = stringResource(R.string.chooseModelToDelete),
        list = vm.translationModels.toList(),
        onClick = { item -> vm.deleteModel(item) },
    ) {
        ListBottomSheetItemModel(
            primaryText = it.language,
            overlineText = try {
                Locale.forLanguageTag(it.language).displayLanguage
            } catch (_: Exception) {
                null
            },
            icon = Icons.Default.Delete
        )
    }
}