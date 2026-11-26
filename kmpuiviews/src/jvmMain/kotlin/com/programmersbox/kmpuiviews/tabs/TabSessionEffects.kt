package com.programmersbox.kmpuiviews.tabs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsEnvironment
import java.awt.Rectangle

/** The main window at its restored position and size; the platform places it when there is none or it is off screen. */
@Composable
fun rememberMainWindowState(window: BrowserWindowState): WindowState {
    val bounds = window.bounds.value
    return rememberWindowState(
        position = bounds?.takeIf(::isOnScreen)?.let { WindowPosition(it.x, it.y) } ?: WindowPosition.PlatformDefault,
        size = initialWindowSize(bounds),
    )
}

/**
 * Keeps [window]'s bounds in step with [windowState] (floating only, so a fullscreen size is never saved), saves the
 * session after changes settle, and queues a last save on [PendingWrites.App] when the app's composition is disposed.
 * Window close and ⌘Q both end it that way, and the caller waits for that queue before the process exits.
 */
@Composable
fun TabSessionEffects(controller: TabsController, store: TabSessionStore, window: BrowserWindowState, windowState: WindowState) {
    LaunchedEffect(window, windowState) {
        snapshotFlow { Triple(windowState.placement, windowState.position, windowState.size) }
            .collect { (placement, position, size) ->
                if (placement == WindowPlacement.Floating && position.isSpecified) {
                    window.bounds.value = WindowBounds(position.x, position.y, size.width, size.height)
                }
            }
    }
    LaunchedEffect(controller, store) { store.autosave(controller) }
    DisposableEffect(controller, store) {
        onDispose {
            val session = controller.toSession(store::encode)
            PendingWrites.App.launch { store.write(session) }
        }
    }
}

/** Whether some of [bounds]' title bar is on a connected screen. AWT and Compose window positions are both in dp. */
private fun isOnScreen(bounds: WindowBounds): Boolean {
    if (GraphicsEnvironment.isHeadless()) return false
    val titleBar = Rectangle(bounds.x.value.toInt(), bounds.y.value.toInt(), bounds.width.value.toInt(), 40)
    return GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
        .any { it.defaultConfiguration.bounds.intersects(titleBar) }
}
