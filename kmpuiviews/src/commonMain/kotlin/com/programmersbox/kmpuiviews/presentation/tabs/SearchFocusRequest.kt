package com.programmersbox.kmpuiviews.presentation.tabs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A pending "focus the search field" request. The desktop tab shell raises it (the browser "address bar"
 * shortcut) and the global search screen consumes it, whether the screen was already showing or is
 * composed a moment later because its tab was just opened.
 */
class SearchFocusRequest {
    var pending: Boolean by mutableStateOf(false)
        private set

    fun request() {
        pending = true
    }

    fun consume() {
        pending = false
    }
}

/** Provided by the desktop tab shell; null everywhere else, so Android and iOS never see it. */
val LocalSearchFocusRequest = staticCompositionLocalOf<SearchFocusRequest?> { null }
