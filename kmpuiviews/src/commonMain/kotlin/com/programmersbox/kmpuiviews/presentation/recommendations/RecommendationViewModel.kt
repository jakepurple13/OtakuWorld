package com.programmersbox.kmpuiviews.presentation.recommendations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.GeminiSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.favoritesdatabase.RecommendationResponse
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class RecommendationViewModel(
    private val aiRecommendationHandler: AiRecommendationHandler,
    private val dao: RecommendationDao,
    newSettingsHandling: NewSettingsHandling,
) : ViewModel() {

    val aiService = newSettingsHandling.aiService

    val geminiSettings = newSettingsHandling.geminiSettings
    val savedRecommendation = dao.getAllRecommendations()

    val messageList = mutableStateListOf<Message>()

    var isLoading by mutableStateOf(false)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        viewModelScope.launch {
            aiRecommendationHandler.init()
        }
    }

    fun send(input: String) {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                messageList.add(Message.User(input))
                val response = aiRecommendationHandler.getResult(input)
                messageList.add(Message.Gemini(json.decodeFromString(response.orEmpty().trim())))
            }.onFailure {
                it.printStackTrace()
                messageList.add(Message.Error(it.message.orEmpty()))
            }
            isLoading = false
        }
    }

    fun insertRecommendation(recommendation: Recommendation) {
        viewModelScope.launch {
            dao.insertRecommendation(recommendation)
        }
    }

    fun deleteRecommendation(recommendation: Recommendation) {
        viewModelScope.launch {
            dao.deleteRecommendation(recommendation.title)
        }
    }

    fun updateGeminiSettings(geminiSettings: GeminiSettings) {
        viewModelScope.launch {
            this@RecommendationViewModel.geminiSettings.set(geminiSettings)
            aiRecommendationHandler.init()
        }
    }
}

sealed class Message {
    data class Gemini(val recommendationResponse: RecommendationResponse) : Message()
    data class User(val text: String) : Message()
    data class Error(val text: String) : Message()
}