package com.programmersbox.kmpuiviews.domain

interface TranslationHandler {
    fun translateDescription(textToTranslate: String, progress: (Boolean) -> Unit, translatedText: (String) -> Unit)

    suspend fun translate(textToTranslate: String): String
    fun clear()
}