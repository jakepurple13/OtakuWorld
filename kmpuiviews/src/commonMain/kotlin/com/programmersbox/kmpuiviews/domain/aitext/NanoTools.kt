package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.flow.Flow

/** Task-specific on-device APIs (e.g. ML Kit GenAI). Bound on Android only. */
interface NanoTools {
    suspend fun isReady(feature: AiFeature, inputLength: Int): Boolean
    fun run(feature: AiFeature, input: String, style: RewriteStyle? = null): Flow<String>
}
