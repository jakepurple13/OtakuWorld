package com.programmersbox.sharedtools

import androidx.navigation3.runtime.NavKey

interface SearchRegistryItem {
    fun addSearchItems(): List<SettingSearchItem>
}

data class SettingSearchItem(
    val displayName: String,
    val keywords: List<String> = emptyList(),
    val breadcrumb: List<NavKey>,
    val targetScreen: NavKey,
    val highlightKey: String,
)
