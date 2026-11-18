package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint
import com.programmersbox.datastore.mangasettings.ReaderType

/** What a reader keyboard shortcut does. The desktop shortcuts dialog lists these in `ReaderShortcuts`. */
internal enum class ReaderKeyAction {
    NextPage,
    PreviousPage,
    ScreenDown,
    ScreenUp,
    FirstPage,
    LastPage,
    NextChapter,
    PreviousChapter,
    ChapterList,
    PageGrid,
    Bookmark,
    SwitchMode,
    ToggleBars,
    Fullscreen,
    Settings,
    ShortcutsList,
    Escape,
}

/**
 * The reader action for a key press, or null to let it through.
 * Keys with Cmd/Ctrl/Alt belong to app shortcuts. Flip and Curl modes turn pages right to left,
 * so ← is next and → is previous there.
 */
internal fun readerKeyAction(
    key: Key,
    shift: Boolean,
    appModifier: Boolean,
    question: Boolean,
    readerType: ReaderType,
): ReaderKeyAction? {
    if (appModifier) return null
    if (question) return ReaderKeyAction.ShortcutsList
    val rightToLeft = readerType == ReaderType.FlipPager || readerType == ReaderType.CurlPager
    return when (key) {
        Key.DirectionDown -> ReaderKeyAction.NextPage
        Key.DirectionUp -> ReaderKeyAction.PreviousPage
        Key.DirectionRight -> if (rightToLeft) ReaderKeyAction.PreviousPage else ReaderKeyAction.NextPage
        Key.DirectionLeft -> if (rightToLeft) ReaderKeyAction.NextPage else ReaderKeyAction.PreviousPage
        Key.Spacebar -> if (shift) ReaderKeyAction.PreviousPage else ReaderKeyAction.NextPage
        Key.PageDown -> ReaderKeyAction.ScreenDown
        Key.PageUp -> ReaderKeyAction.ScreenUp
        Key.MoveHome -> ReaderKeyAction.FirstPage
        Key.MoveEnd -> ReaderKeyAction.LastPage
        Key.RightBracket -> ReaderKeyAction.NextChapter
        Key.LeftBracket -> ReaderKeyAction.PreviousChapter
        Key.C -> ReaderKeyAction.ChapterList
        Key.G -> ReaderKeyAction.PageGrid
        Key.B -> ReaderKeyAction.Bookmark
        Key.M -> ReaderKeyAction.SwitchMode
        Key.H -> ReaderKeyAction.ToggleBars
        Key.F -> ReaderKeyAction.Fullscreen
        Key.Comma -> ReaderKeyAction.Settings
        Key.Escape -> ReaderKeyAction.Escape
        else -> null
    }
}

internal fun KeyEvent.readerKeyAction(readerType: ReaderType): ReaderKeyAction? = readerKeyAction(
    key = key,
    shift = isShiftPressed,
    appModifier = isMetaPressed || isCtrlPressed || isAltPressed,
    question = utf16CodePoint == '?'.code || (key == Key.Slash && isShiftPressed),
    readerType = readerType,
)

private val ReaderTypeCycle = listOf(ReaderType.List, ReaderType.Pager, ReaderType.FlipPager, ReaderType.CurlPager)

internal fun ReaderType.nextReaderType(): ReaderType =
    ReaderTypeCycle[(ReaderTypeCycle.indexOf(this) + 1) % ReaderTypeCycle.size]

/** Window-level reader actions; null where the platform has no such thing (fullscreen and the shortcuts dialog are desktop only). */
internal class ReaderPlatformActions(
    val toggleFullscreen: (() -> Unit)?,
    /** Leaves fullscreen; false when it wasn't on. */
    val exitFullscreen: () -> Boolean,
    val openShortcuts: (() -> Unit)?,
)

@Composable
internal expect fun rememberReaderPlatformActions(): ReaderPlatformActions
