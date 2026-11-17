package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class RewriteStyle { Rephrase, Shorten, Elaborate, Friendly, Professional, Emojify }

data class AiEngineInfo(val label: String, val onDevice: Boolean, val destination: String?)

sealed interface AiTextState {
    data object Idle : AiTextState
    data object Loading : AiTextState
    data class Streaming(val text: String, val engine: AiEngineInfo) : AiTextState
    data class Done(val text: String, val engine: AiEngineInfo) : AiTextState
    /** [message] is for logs only; the UI shows the localized `ai_error` string. */
    data class Error(val message: String) : AiTextState
}

interface AiText {
    /**
     * Null when no on-device engine is ready for [feature] with input of [inputLength] characters
     * (the length after [AiFeature.prepare]). Re-checks status on every call.
     */
    suspend fun engineFor(feature: AiFeature<*>, inputLength: Int = 0): AiEngineInfo?

    /** Streams the reply as deltas. Throws [IllegalStateException] on collect when no engine is ready. */
    fun <I> run(feature: AiFeature<I>, input: I): Flow<String>

    /**
     * Emits null and re-checks every [retry] until an engine is ready, then emits it and completes.
     * Nano can still be downloading, or AICore not bound yet, when a screen opens.
     */
    fun availability(
        feature: AiFeature<*>,
        inputLength: Int = 0,
        retry: Duration = 2.seconds,
    ): Flow<AiEngineInfo?> = flow {
        while (true) {
            val engine = engineFor(feature, inputLength)
            emit(engine)
            if (engine != null) return@flow
            delay(retry)
        }
    }
}
