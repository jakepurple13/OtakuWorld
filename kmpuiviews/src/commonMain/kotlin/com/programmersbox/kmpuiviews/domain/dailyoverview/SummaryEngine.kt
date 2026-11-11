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

    /** True when nothing leaves the device. */
    val onDevice: Boolean

    suspend fun status(): EngineStatus

    fun summarize(facts: DailyFacts): Flow<String>
}
