package com.programmersbox.kmpuiviews.presentation.urlopener

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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

    val sourceList = mutableStateMapOf<String, List<KmpSourceInformation>>()

    var currentChosenSource by mutableStateOf<KmpSourceInformation?>(null)

    var kmpItemModel by mutableStateOf<KmpItemModel?>(null)

    init {
        sourceRepository
            .sources
            .onEach { list ->
                sourceList.putAll(
                    list
                        .filterNot { it.apiService.notWorking }
                        .distinctBy { it.packageName }
                        .groupBy { it.packageName }
                )
                currentChosenSource = sourceList
                    .entries
                    .randomOrNull()
                    ?.value
                    ?.firstOrNull()
            }
            .launchIn(viewModelScope)
    }

    fun open(url: String) {
        viewModelScope.launch {
            kmpItemModel = runCatching { currentChosenSource?.apiService?.sourceByUrl(url) }.getOrNull()
        }
    }
}