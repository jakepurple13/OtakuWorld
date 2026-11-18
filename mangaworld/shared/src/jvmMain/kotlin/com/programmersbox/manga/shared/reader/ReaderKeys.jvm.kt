package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.window.WindowPlacement
import com.programmersbox.kmpuiviews.presentation.quickactions.QuickActionsController
import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutEntry
import com.programmersbox.kmpuiviews.presentation.shortcuts.ShortcutSection
import org.koin.compose.koinInject

/** Fullscreen of the window hosting the reader; left again when the reader leaves composition. */
private class ReaderFullscreen(private val window: ComposeWindow) {
    private var restorePlacement = WindowPlacement.Floating
    private var isOn = window.placement == WindowPlacement.Fullscreen

    fun toggle() {
        if (isOn) {
            window.placement = restorePlacement
        } else {
            restorePlacement = window.placement
            window.placement = WindowPlacement.Fullscreen
        }
        isOn = !isOn
    }

    fun exit(): Boolean {
        if (!isOn) return false
        toggle()
        return true
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun rememberReaderPlatformActions(): ReaderPlatformActions {
    val window = LocalAwtWindow.current as? ComposeWindow
    val quickActions = koinInject<QuickActionsController>()
    val fullscreen = remember(window) { window?.let(::ReaderFullscreen) }
    DisposableEffect(fullscreen) {
        onDispose { fullscreen?.exit() }
    }
    return remember(fullscreen, quickActions) {
        ReaderPlatformActions(
            toggleFullscreen = fullscreen?.let { it::toggle },
            exitFullscreen = { fullscreen?.exit() == true },
            openShortcuts = quickActions::openShortcuts,
        )
    }
}

/** Keep in step with [readerKeyAction]. */
internal val ReaderShortcuts = listOf(
    ShortcutEntry(listOf("↓", "→", "Space"), "Next page"),
    ShortcutEntry(listOf("↑", "←", "Shift+Space"), "Previous page"),
    ShortcutEntry(listOf("←", "→"), "Next / previous page in Flip and Curl"),
    ShortcutEntry(listOf("PgDn", "PgUp"), "Scroll one screen"),
    ShortcutEntry(listOf("Home", "End"), "First / last page"),
    ShortcutEntry(listOf("]", "["), "Next / previous chapter"),
    ShortcutEntry(listOf("C"), "Chapter list"),
    ShortcutEntry(listOf("G"), "Page grid"),
    ShortcutEntry(listOf("B"), "Bookmark chapter"),
    ShortcutEntry(listOf("M"), "Switch reader mode"),
    ShortcutEntry(listOf("H", "click"), "Show / hide bars"),
    ShortcutEntry(listOf("F"), "Fullscreen"),
    ShortcutEntry(listOf(","), "Reader settings"),
    ShortcutEntry(listOf("?"), "This list", listLabel = "Reader shortcuts"),
    ShortcutEntry(listOf("Esc"), "Close / leave reader"),
)

fun readerShortcutSection() = ShortcutSection("Manga reader", ReaderShortcuts)
