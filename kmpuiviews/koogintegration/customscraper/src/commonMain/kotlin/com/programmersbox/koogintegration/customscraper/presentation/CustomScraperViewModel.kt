package com.programmersbox.koogintegration.customscraper.presentation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.koogintegration.customscraper.model.CustomScrapeKmpInfoModel
import com.programmersbox.koogintegration.customscraper.repository.CustomScraperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class CustomScraperViewModel(
    private val scraperRepository: CustomScraperRepository,
) : ViewModel() {

    val currentTextFieldState = TextFieldState()

    val uiState: StateFlow<CustomScraperState>
        field = MutableStateFlow(CustomScraperState())

    fun extractDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = uiState.value.copy(isLoading = true)
            val result = scraperRepository.scrapeDetails(currentTextFieldState.text.toString())
            uiState.value = uiState.value.copy(isLoading = false, customScrapeKmpInfoModel = result)
        }
    }
}

@Serializable
@Stable
data class CustomScraperState(
    val isLoading: Boolean = false,
    val customScrapeKmpInfoModel: CustomScrapeKmpInfoModel? = null,
)