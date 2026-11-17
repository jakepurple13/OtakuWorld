package com.programmersbox.kmpuiviews.domain.aitext

import kotlinx.coroutines.flow.Flow

/** Task-specific on-device APIs (e.g. ML Kit GenAI). Bound on Android only. */
interface NanoTools {
    suspend fun isReady(task: NanoTask, inputLength: Int): Boolean
    fun run(task: NanoTask, input: String): Flow<String>
}
