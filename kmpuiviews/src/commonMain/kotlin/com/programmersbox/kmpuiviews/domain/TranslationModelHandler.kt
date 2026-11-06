package com.programmersbox.kmpuiviews.domain

import kotlinx.coroutines.flow.StateFlow

interface TranslationModelHandler {
    /** False on builds without a translator (noFirebase). */
    val isAvailable: Boolean

    /** One row per language the engine knows, downloaded or not. */
    val models: StateFlow<List<TranslationModel>>
    suspend fun refresh()
    fun download(code: String)
    fun cancelDownload(code: String)
    suspend fun delete(codes: Set<String>)
}

data class TranslationModel(
    val language: TranslationLanguage,
    val state: ModelState,
    val sizeBytes: Long?, // null = unknown
    val sizeIsEstimate: Boolean,
    val required: Boolean, // English on ML Kit
    val lastUsedMillis: Long?,
)

sealed interface ModelState {
    data object NotDownloaded : ModelState
    data class Downloading(val fraction: Float?) : ModelState // null = indeterminate
    data object Downloaded : ModelState
    data class Failed(val message: String) : ModelState
}
