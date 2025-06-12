package com.programmersbox.kmpuiviews.presentation.recommendations

import com.programmersbox.datastore.NewSettingsHandling
import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.GenerativeModel
import dev.shreyaspatil.ai.client.generativeai.type.BlockThreshold
import dev.shreyaspatil.ai.client.generativeai.type.HarmCategory
import dev.shreyaspatil.ai.client.generativeai.type.SafetySetting
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import dev.shreyaspatil.ai.client.generativeai.type.content
import dev.shreyaspatil.ai.client.generativeai.type.generationConfig

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

//TODO: Maaaaybe make this an interface so the user can choose what backend they want?
class AiRecommendationHandler(
    private val settingsHandling: NewSettingsHandling,
) {
    private var generativeModel: GenerativeModel? = null
    private var chat: Chat? = null

    suspend fun init() {
        val geminiSettings = settingsHandling.geminiSettings.get()
        println(geminiSettings)
        generativeModel = GenerativeModel(
            geminiSettings.modelName,
            geminiSettings.apiKey,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "application/json"
                responseSchema = schema
            },
            safetySettings = SAFETY_SETTINGS,
            systemInstruction = content { text(geminiSettings.prompt) },
        )
        chat = generativeModel?.startChat()
    }


    suspend fun getResult(prompt: String): String? {
        return chat?.sendMessage(prompt)?.text
    }
}