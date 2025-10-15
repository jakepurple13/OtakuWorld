package com.programmersbox.uiviews

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.PlatformGenericInfo

interface GenericInfo : PlatformGenericInfo {
    context(navGraph: EntryProviderScope<NavKey>)
    fun globalNav3Setup() {
    }

    context(navGraph: EntryProviderScope<NavKey>)
    fun settingsNav3Setup() {
    }
}