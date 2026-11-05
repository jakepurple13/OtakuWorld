package com.programmersbox.kmpuiviews.presentation.quickactions

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks
import otakuworld.kmpuiviews.generated.resources.qa_check_updates
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr
import otakuworld.kmpuiviews.generated.resources.quick_actions

val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

/** Cmd+K on macOS, Ctrl+K elsewhere, with no other modifiers. */
fun isQuickActionsShortcut(event: KeyEvent, isMac: Boolean): Boolean =
    event.type == KeyEventType.KeyDown &&
            event.key == Key.K &&
            !event.isShiftPressed &&
            !event.isAltPressed &&
            if (isMac) event.isMetaPressed && !event.isCtrlPressed
            else event.isCtrlPressed && !event.isMetaPressed

private fun QuickAction.label(): StringResource = when (this) {
    QuickAction.CheckForUpdates -> Res.string.qa_check_updates
    QuickAction.ScanQrCode -> Res.string.qa_scan_qr
    QuickAction.BackgroundTasks -> Res.string.qa_background_tasks
}

@Composable
fun FrameWindowScope.QuickActionsMenuBar() {
    val controller = koinInject<QuickActionsController>()
    val viewModel = koinViewModel<QuickActionsViewModel>()
    val actionsTitle = stringResource(Res.string.quick_actions)
    val labels = QuickAction.entries.map { it to stringResource(it.label()) }
    MenuBar {
        Menu("Actions") {
            labels.forEach { (action, label) ->
                Item(label, onClick = { viewModel.run(action) })
            }
            Separator()
            Item(
                "$actionsTitle…",
                shortcut = KeyShortcut(Key.K, meta = true),
                onClick = { controller.open(OpenOrigin.Keyboard) },
            )
        }
    }
}

@Composable
fun QuickActionsTitleBarMenu() {
    val controller = koinInject<QuickActionsController>()
    val viewModel = koinViewModel<QuickActionsViewModel>()
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) { Text("Actions") }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        QuickAction.entries.forEach { action ->
            DropdownMenuItem(
                text = { Text(stringResource(action.label())) },
                onClick = {
                    expanded = false
                    viewModel.run(action)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.quick_actions) + "…  Ctrl+K") },
            onClick = {
                expanded = false
                controller.open(OpenOrigin.Keyboard)
            },
        )
    }
}
