package com.programmersbox.koogintegration

import com.programmersbox.sharedtools.SearchRegistryItem
import com.programmersbox.sharedtools.SettingSearchItem

class KoogSearchItems : SearchRegistryItem {
    override fun addSearchItems(): List<SettingSearchItem> {
        return listOf(
            SettingSearchItem(
                displayName = "Koog",
                keywords = listOf("koog", "ai", "recommendations", "analyze"),
                breadcrumb = listOf(Koog),
                targetScreen = Koog,
                highlightKey = "koog",
            ),
            SettingSearchItem(
                displayName = "Koog Settings",
                keywords = listOf("koog", "configuration", "settings"),
                breadcrumb = listOf(Koog, KoogSettings),
                targetScreen = KoogSettings,
                highlightKey = "koog_settings",
            )
        )
    }
}