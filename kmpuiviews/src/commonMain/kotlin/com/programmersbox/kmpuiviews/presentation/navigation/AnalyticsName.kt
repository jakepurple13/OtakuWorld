package com.programmersbox.kmpuiviews.presentation.navigation

import androidx.navigation3.runtime.NavKey
import com.programmersbox.kmpuiviews.presentation.Screen

/**
 * Name sent to analytics and crash logs for a destination.
 * Never includes a key's properties, which can hold titles and URLs.
 */
fun NavKey?.analyticsName(): String = when (this) {
    null -> "none"
    is Screen -> route
    else -> this::class.simpleName ?: "unknown"
}
