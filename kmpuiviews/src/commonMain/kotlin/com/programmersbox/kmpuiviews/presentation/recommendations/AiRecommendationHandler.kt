package com.programmersbox.kmpuiviews.presentation.recommendations

interface AiRecommendationHandler {
    suspend fun init()
    suspend fun getResult(prompt: String): String?
}