package com.programmersbox.uiviews

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.PlatformGenericInfo

interface GenericInfo : PlatformGenericInfo {
    context(navGraph: EntryProviderBuilder<NavKey>)
    fun globalNav3Setup() {
    }

    context(navGraph: EntryProviderBuilder<NavKey>)
    fun settingsNav3Setup() {
    }
}