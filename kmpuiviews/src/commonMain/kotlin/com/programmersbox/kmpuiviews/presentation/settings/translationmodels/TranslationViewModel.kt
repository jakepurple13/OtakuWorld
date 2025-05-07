package com.programmersbox.kmpuiviews.presentation.settings.translationmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.domain.KmpCustomRemoteModel
import com.programmersbox.kmpuiviews.domain.TranslationModelHandler
import kotlinx.coroutines.launch

class TranslationViewModel(
    private val translationModelHandler: TranslationModelHandler,
) : ViewModel() {

    var translationModels: List<KmpCustomRemoteModel> by mutableStateOf(emptyList())
        private set

    fun loadModels() {
        viewModelScope.launch {
            translationModels = translationModelHandler.modelList()
        }
    }

    fun deleteModel(model: KmpCustomRemoteModel) {
        viewModelScope.launch {
            translationModelHandler.delete(model)
            translationModels = translationModelHandler.modelList()
        }
    }
}