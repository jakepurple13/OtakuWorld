package com.programmersbox.sharedcomponents

import androidx.navigation3.runtime.NavKey

interface Navigator {
    fun navigate(route: NavKey)
}