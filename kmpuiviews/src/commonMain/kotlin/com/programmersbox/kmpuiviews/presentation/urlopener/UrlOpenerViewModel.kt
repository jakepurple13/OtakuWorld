package com.programmersbox.kmpuiviews.presentation.urlopener

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class UrlOpenerViewModel(
    sourceRepository: SourceRepository,
) : ViewModel() {

    val sourceList = mutableStateListOf<KmpSourceInformation>()

    var currentChosenSource by mutableStateOf<KmpSourceInformation?>(null)

    var kmpItemModel by mutableStateOf<KmpItemModel?>(null)

    init {
        sourceRepository
            .sources
            .onEach {
                sourceList.addAll(it.filter { it.apiService.notWorking })
                currentChosenSource = sourceList.firstOrNull()
            }
            .launchIn(viewModelScope)
    }

    fun open(url: String) {
        viewModelScope.launch {
            kmpItemModel = runCatching { currentChosenSource?.apiService?.sourceByUrl(url) }.getOrNull()
        }
    }
}