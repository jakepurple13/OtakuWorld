package com.programmersbox.sharedcomponents.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@Stable
data class StatData(
    val id: String,
    val label: String,
    val description: String,
    val value: String,
    val valueColor: @Composable () -> Color = { Color.Unspecified },
)

@Stable
data class StatInfo(
    val header: String,
    val key: String,
    val contentType: String,
    val priority: Int,
    val stats: List<StatData>,
)

abstract class StatisticsProvider {
    abstract val header: String
    abstract val key: String
    abstract val contentType: String
    abstract val priority: Int
    abstract fun observeStats(): Flow<List<StatData>>

    fun getStats(): Flow<StatInfo> {
        return observeStats()
            .map { StatInfo(header, key, contentType, priority, it) }
            .onStart { emit(StatInfo(header, key, contentType, priority, listOf())) }
            .flowOn(Dispatchers.IO)
    }
}