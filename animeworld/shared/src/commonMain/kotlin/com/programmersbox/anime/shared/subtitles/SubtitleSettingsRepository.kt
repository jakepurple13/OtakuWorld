package com.programmersbox.anime.shared.subtitles

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SubtitleSettingsRepository {
    val style: Flow<SubtitleStyle>
    suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle)
    suspend fun reset()
}

/** Non-persistent implementation for Desktop/iOS. */
class InMemorySubtitleSettingsRepository : SubtitleSettingsRepository {
    private val state = MutableStateFlow(SubtitleStyle())
    override val style: Flow<SubtitleStyle> = state.asStateFlow()

    override suspend fun update(transform: (SubtitleStyle) -> SubtitleStyle) {
        state.update { transform(it).coerced() }
    }

    override suspend fun reset() {
        state.value = SubtitleStyle()
    }
}
