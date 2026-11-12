package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.flow.Flow

enum class AiFeature { Recap, Tldr, Proofread, Rewrite, ListSuggest }

enum class RewriteStyle { Rephrase, Shorten, Elaborate, Friendly, Professional, Emojify }

data class AiEngineInfo(val label: String, val onDevice: Boolean, val destination: String?)

sealed interface AiTextState {
    data object Idle : AiTextState
    data object Loading : AiTextState
    data class Streaming(val text: String, val engine: AiEngineInfo) : AiTextState
    data class Done(val text: String, val engine: AiEngineInfo) : AiTextState
    data class Error(val message: String) : AiTextState
}

interface AiText {
    /** Null when no on-device engine is ready for [feature]. Re-checks status on every call. */
    suspend fun engineFor(feature: AiFeature, inputLength: Int = 0): AiEngineInfo?
    fun recap(previousChapterText: String): Flow<String>
    fun tldr(description: String): Flow<String>          // emits bullet text, one bullet per line
    fun proofread(text: String): Flow<String>
    fun rewrite(text: String, style: RewriteStyle): Flow<String>
    fun listNames(titles: List<String>): Flow<String>    // one name per line
    fun listDescription(titles: List<String>): Flow<String>
}
