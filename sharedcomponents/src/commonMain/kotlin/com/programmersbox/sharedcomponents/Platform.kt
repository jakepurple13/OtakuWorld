package com.programmersbox.sharedcomponents

import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun navigateTo(route: NavKey)
    fun onBack()
    fun toCustomList()
    fun toGlobalSearch(query: String)
}