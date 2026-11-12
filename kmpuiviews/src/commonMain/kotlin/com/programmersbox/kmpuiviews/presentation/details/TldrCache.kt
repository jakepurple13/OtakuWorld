package com.programmersbox.kmpuiviews.presentation.details

import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A finished TL;DR and whether it was summarized from a translated description. */
data class TldrEntry(val done: AiTextState.Done, val translated: Boolean)

/** Finished TL;DRs keyed by item URL, kept in memory for the app session. */
class TldrCache {
    private val entries = MutableStateFlow<Map<String, TldrEntry>>(emptyMap())

    operator fun get(url: String): TldrEntry? = entries.value[url]

    operator fun set(url: String, entry: TldrEntry) {
        entries.update { it + (url to entry) }
    }
}
