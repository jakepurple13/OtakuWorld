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
    // Not a Screen, and R8 can rename its class in minified builds; it is the busiest destination
    is Screen.DetailsScreen.Details -> "details"
    // Other keys keep their class name through the -keepnames rule in UIViews/consumer-rules.pro
    else -> this::class.simpleName ?: "unknown"
}
