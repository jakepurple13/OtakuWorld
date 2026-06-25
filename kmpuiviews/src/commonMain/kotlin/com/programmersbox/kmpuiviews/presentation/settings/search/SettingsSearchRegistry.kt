package com.programmersbox.kmpuiviews.presentation.settings.search

class SettingsSearchRegistry(val items: List<SettingsSearchItem>) {

    fun search(query: String): List<SettingsSearchItem> {
        if (query.isBlank()) return emptyList()
        return items.filter { item ->
            item.displayName.contains(query, ignoreCase = true)
                || item.keywords.any { it.contains(query, ignoreCase = true) }
        }
    }
}

// Completed in Task 15 once all screens and their Screen.* keys exist
fun builtInSettingsItems(): List<SettingsSearchItem> = emptyList()
