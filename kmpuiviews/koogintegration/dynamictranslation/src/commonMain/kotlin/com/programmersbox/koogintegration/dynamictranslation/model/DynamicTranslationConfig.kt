package com.programmersbox.koogintegration.dynamictranslation.model

import kotlinx.serialization.Serializable

@Serializable
data class DynamicTranslationConfig(
    val sourceLanguage: String,
    val targetLanguage: String,
    val tessDataPath: String,
    val ollamaModel: String = "llama3.2",
    val nllbModelPath: String = "",
)
