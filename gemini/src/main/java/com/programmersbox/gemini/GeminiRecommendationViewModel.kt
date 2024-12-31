package com.programmersbox.gemini

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val HARASSMENT_PARAM = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE)
private val HATE_SPEECH_PARAM = SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE)
private val DANGEROUS_CONTENT_PARAM =
    SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
private val SEXUALLY_EXPLICIT_PARAM =
    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE)
private val SAFETY_SETTINGS =
    listOf(HARASSMENT_PARAM, HATE_SPEECH_PARAM, DANGEROUS_CONTENT_PARAM, SEXUALLY_EXPLICIT_PARAM)

private val schema = Schema.obj(
    "response",
    "a response",
    Schema.arr(
        "recommendations",
        "a list of recommendations",
        Schema.obj(
            "recommendation",
            "a single recommendation",
            Schema.str("title", "the title of the recommendation"),
            Schema.str("description", "a short description of the recommendation"),
            Schema.str("reason", "a short reason for the recommendation"),
            Schema.arr("genre", "a list of genres", Schema.str("genre", "a genre"))
        )
    )
)

private const val prompt =
    "You are a human-like, minimalistic bot speaking to adults who really like anime, manga, and novels. They are asking about recommendations based on what they have currently read or watched or just random recommendations in general. When responding, make sure to include a response, the title, a short summary without any spoilers, the reason why you recommended it, and a few genre tags for the recommendation. Try to recommend at least 3 per response.\nWhen responding, respond with json like the following:\n{\"response\":response,\"recommendations\":[{\"title\":title, \"description\":description, \"reason\": reason, genre:[genres]}]}"

class GeminiRecommendationViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        "gemini-1.5-flash",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
            responseSchema = schema
        },
        // safetySettings = Adjust safety settings
        // See https://ai.google.dev/gemini-api/docs/safety-settings
        safetySettings = SAFETY_SETTINGS,
        systemInstruction = content { text(prompt) },
    )

    private val chat = generativeModel.startChat()

    val messageList = mutableStateListOf<Message>()

    var isLoading by mutableStateOf(false)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun send(input: String) {
        viewModelScope.launch {
            isLoading = true
            runCatching {
                messageList.add(Message.User(input))
                val response = chat.sendMessage(input)
                messageList.add(Message.Gemini(json.decodeFromString(response.text.orEmpty().trim())))
            }.onFailure {
                it.printStackTrace()
                messageList.add(Message.Error(it.localizedMessage.orEmpty()))
            }
            isLoading = false
        }
    }
}

sealed class Message {
    data class Gemini(val recommendationResponse: RecommendationResponse) : Message()
    data class User(val text: String) : Message()
    data class Error(val text: String) : Message()
}
