package com.programmersbox.kmpuiviews.presentation.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Opens things in desktop tabs. Provided per tab by the desktop tab shell; null everywhere else,
 * so Android and iOS never see it.
 */
interface TabActions {
    /**
     * Adds open-in-tab gestures to an element that runs [onOpen] when clicked: modifier-click and
     * middle-click open a new tab, right-click shows a menu. [onMore], when given, adds a "More…" entry.
     */
    @Composable
    fun Modifier.target(onOpen: () -> Unit, onMore: (() -> Unit)?): Modifier
}

val LocalTabActions = staticCompositionLocalOf<TabActions?> { null }

/** Lets this element open in a new tab on desktop. Returns this modifier unchanged without [LocalTabActions]. */
@Composable
fun Modifier.tabTarget(onOpen: () -> Unit, onMore: (() -> Unit)? = null): Modifier {
    val actions = LocalTabActions.current ?: return this
    return with(actions) { target(onOpen, onMore) }
}
