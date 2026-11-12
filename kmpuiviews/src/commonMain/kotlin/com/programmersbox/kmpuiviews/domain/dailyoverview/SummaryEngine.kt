package com.programmersbox.kmpuiviews.domain.dailyoverview

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
enum class SummaryEngineId { Nano, Gemma, Provider, Ollama, Template }

sealed interface EngineStatus {
    data object Ready : EngineStatus
    data class NeedsSetup(val reason: String) : EngineStatus
    data object Unavailable : EngineStatus
}

interface SummaryEngine {
    val id: SummaryEngineId

    /** Shown on the card, e.g. "Gemma 4 E2B · on device". */
    suspend fun label(): String

    /** Shown in settings rows, e.g. "Gemma 4 E2B" or "Google · gemini-2.5-flash". */
    suspend fun shortName(): String = label()

    /** True when nothing leaves the device. */
    val onDevice: Boolean

    /** Who receives the facts when [onDevice] is false, e.g. "Google". Null when nothing is sent. */
    suspend fun destination(): String? = null

    suspend fun status(): EngineStatus

    fun summarize(facts: DailyFacts): Flow<String>

    /** Runs a free-form prompt and emits the reply as streamed deltas. */
    fun generate(system: String, user: String): Flow<String>
}
