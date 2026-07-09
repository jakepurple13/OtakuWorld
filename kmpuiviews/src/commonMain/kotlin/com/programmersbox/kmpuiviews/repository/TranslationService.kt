package com.programmersbox.kmpuiviews.repository

data class TranslationResult(
    val translatedTerm: String,
    val translatedDefinition: String?,
    val reading: String?,
)

interface TranslationService {
    suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult
}

class StubTranslationService : TranslationService {
    override suspend fun translateTerm(
        term: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslationResult = TranslationResult(
        translatedTerm = term,
        translatedDefinition = "Stub translation not yet implemented.",
        reading = null,
    )
}
