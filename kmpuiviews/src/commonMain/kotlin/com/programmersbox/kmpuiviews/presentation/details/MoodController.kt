package com.programmersbox.kmpuiviews.presentation.details

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.moodtags.MoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Mood row on a favorite's Details page. Moods are added by hand or with the AI chip; nothing
 * generates on its own. The AI switch only affects [canSuggest].
 */
class MoodController(
    private val repository: MoodRepository,
    aiText: AiText,
    private val scope: CoroutineScope,
    private val url: String?,
    isFavorite: Flow<Boolean>,
) {
    val moods: StateFlow<List<String>> = (url?.let(repository::moods) ?: emptyFlow())
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The user's own moods, offered in the Add sheet for every title. */
    val customMoods: StateFlow<List<String>> = repository.customMoods
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The row shows only for favorites. */
    val visible: StateFlow<Boolean> = isFavorite.stateIn(scope, SharingStarted.Eagerly, false)

    private val _suggesting = MutableStateFlow(false)
    val suggesting: StateFlow<Boolean> = _suggesting.asStateFlow()

    private val _sheetOpen = MutableStateFlow(false)

    /** The ＋ Add sheet. Kept here so it survives rotation (portrait and landscape are different composables). */
    val sheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

    fun openSheet() {
        _sheetOpen.value = true
    }

    fun closeSheet() {
        _sheetOpen.value = false
    }

    private val _suggestFailed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the AI couldn't suggest anything usable, so the screen can say so. */
    val suggestFailed: SharedFlow<Unit> = _suggestFailed.asSharedFlow()

    /** The AI chip: a favorite with no moods, an engine ready and the switch on, and not already working. */
    val canSuggest: StateFlow<Boolean> = combine(
        visible,
        moods,
        aiText.availability(AiFeature.MoodTags),
        _suggesting,
    ) { favorite, moods, engine, suggesting -> favorite && moods.isEmpty() && engine != null && !suggesting }
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun suggest(title: String, description: String) {
        val url = url ?: return
        if (_suggesting.value) return
        _suggesting.value = true
        scope.launch {
            try {
                if (!repository.suggest(url, title, description)) _suggestFailed.tryEmit(Unit)
            } finally {
                _suggesting.value = false
            }
        }
    }

    fun toggle(mood: String) = edit { repository.toggle(it, mood) }
    fun remove(mood: String) = edit { repository.remove(it, mood) }
    fun create(name: String) = edit { repository.create(it, name) }

    private fun edit(block: suspend (String) -> Unit) {
        val url = url ?: return
        scope.launch { block(url) }
    }
}
