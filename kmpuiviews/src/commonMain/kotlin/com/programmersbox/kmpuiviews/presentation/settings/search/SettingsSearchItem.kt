package com.programmersbox.kmpuiviews.presentation.settings.search

import androidx.navigation3.runtime.NavKey

data class SettingsSearchItem(
    val displayName: String,
    val keywords: List<String> = emptyList(),
    val breadcrumb: List<NavKey>,
    val targetScreen: NavKey,
    val highlightKey: String,
)

class SettingsHighlightState {
    var pendingHighlightKey: String? = null
}
