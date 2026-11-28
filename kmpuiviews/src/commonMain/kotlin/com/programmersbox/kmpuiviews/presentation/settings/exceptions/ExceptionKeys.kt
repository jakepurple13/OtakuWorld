package com.programmersbox.kmpuiviews.presentation.settings.exceptions

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

enum class ExceptionKeyAction { Up, Down, Delete, Copy, Undo }

/** The parts of a key event the shortcuts look at. */
data class KeyPress(val key: Key, val type: KeyEventType, val meta: Boolean, val ctrl: Boolean)

fun KeyEvent.toKeyPress() = KeyPress(key, type, isMetaPressed, isCtrlPressed)

/** Timeline shortcuts (two-pane): ↑/↓ select, ⌫/Delete delete, ⌘C/Ctrl+C copy, ⌘Z/Ctrl+Z undo. */
fun exceptionKeyAction(press: KeyPress): ExceptionKeyAction? {
    if (press.type != KeyEventType.KeyDown) return null
    val command = press.meta || press.ctrl
    return when (press.key) {
        Key.DirectionUp -> ExceptionKeyAction.Up
        Key.DirectionDown -> ExceptionKeyAction.Down
        Key.Backspace, Key.Delete -> ExceptionKeyAction.Delete
        Key.C -> ExceptionKeyAction.Copy.takeIf { command }
        Key.Z -> ExceptionKeyAction.Undo.takeIf { command }
        else -> null
    }
}

/** Right-click menu on desktop; Android and iOS just show [content]. */
@Composable
expect fun RowContextMenu(
    onCopy: () -> Unit,
    onSave: () -> Unit,
    onOpenWebsite: (() -> Unit)?,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
)

/** The shortcut hint under the timeline, or null where there is no keyboard (Android). */
@Composable
expect fun keyboardHint(): String?
