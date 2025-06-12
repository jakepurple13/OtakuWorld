package com.programmersbox.kmpuiviews.presentation.recommendations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.AiSettings
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.Recommendation
import com.programmersbox.favoritesdatabase.RecommendationDao
import com.programmersbox.favoritesdatabase.RecommendationResponse
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

class RecommendationViewModel(
    private val dao: RecommendationDao,
    newSettingsHandling: NewSettingsHandling,
) : ViewModel(), KoinComponent {

    private var aiRecommendationHandler: AiRecommendationHandler? = null
    val aiSettings = newSettingsHandling.aiSettings
    val savedRecommendation = dao.getAllRecommendations()

    val messageList = mutableStateListOf<Message>()

    var isLoading by mutableStateOf(false)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    init {
        aiSettings
            .asFlow()
            .map { it.aiService }
            .map { get<AiRecommendationHandler>(named(it.name)) }
            .onEach {
                isLoading = true
                it.init()
                aiRecommendationHandler = it
                messageList.clear()
                isLoading = false
            }
            .launchIn(viewModelScope)
    }

    fun send(input: String) {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                messageList.add(Message.User(input))
                val response = aiRecommendationHandler?.getResult(input)
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

    fun updateSettings(aiSettings: AiSettings) {
        viewModelScope.launch {
            this@RecommendationViewModel.aiSettings.set(aiSettings)
        }
    }
}

sealed class Message {
    data class Gemini(val recommendationResponse: RecommendationResponse) : Message()
    data class User(val text: String) : Message()
    data class Error(val text: String) : Message()
}