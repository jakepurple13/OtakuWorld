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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.programmersbox.kmpuiviews.presentation.shortcuts.AppShortcut
import com.programmersbox.kmpuiviews.presentation.shortcuts.label
import com.programmersbox.kmpuiviews.presentation.shortcuts.matches
import com.programmersbox.kmpuiviews.presentation.shortcuts.menuShortcut
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.qa_background_tasks
import otakuworld.kmpuiviews.generated.resources.qa_check_updates
import otakuworld.kmpuiviews.generated.resources.qa_scan_qr
import otakuworld.kmpuiviews.generated.resources.qa_update_errors
import otakuworld.kmpuiviews.generated.resources.qa_global_search
import otakuworld.kmpuiviews.generated.resources.qa_lists
import otakuworld.kmpuiviews.generated.resources.qa_saved_notifications
import otakuworld.kmpuiviews.generated.resources.qa_menu_actions
import otakuworld.kmpuiviews.generated.resources.qa_menu_open
import otakuworld.kmpuiviews.generated.resources.qa_menu_open_shortcut
import otakuworld.kmpuiviews.generated.resources.qa_menu_shortcuts

internal val isMacOs: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

/** Cmd+K on macOS, Ctrl+K elsewhere, with no other modifiers. */
fun isQuickActionsShortcut(event: KeyEvent, isMac: Boolean): Boolean =
    AppShortcut.QuickActions.matches(event, isMac)

private fun QuickAction.label(): StringResource = when (this) {
    QuickAction.CheckForUpdates -> Res.string.qa_check_updates
    QuickAction.ScanQrCode -> Res.string.qa_scan_qr
    QuickAction.GlobalSearch -> Res.string.qa_global_search
    QuickAction.Lists -> Res.string.qa_lists
    QuickAction.SavedNotifications -> Res.string.qa_saved_notifications
    QuickAction.BackgroundTasks -> Res.string.qa_background_tasks
    QuickAction.UpdateErrors -> Res.string.qa_update_errors
}

@Composable
fun FrameWindowScope.QuickActionsMenuBar() {
    val controller = koinInject<QuickActionsController>()
    // Not scoped to this menu: it resolves to the same ViewModelStoreOwner as QuickActionsOverlay,
    // so it shares the overlay's instance and a run started here shows the overlay's snackbar.
    val viewModel = koinViewModel<QuickActionsViewModel>()
    val menuTitle = stringResource(Res.string.qa_menu_actions)
    val openLabel = stringResource(Res.string.qa_menu_open)
    val shortcutsLabel = stringResource(Res.string.qa_menu_shortcuts)
    val labels = QuickAction.entries.map { it to stringResource(it.label()) }
    MenuBar {
        Menu(menuTitle) {
            labels.forEachIndexed { index, (action, label) ->
                if (index > 0 && QuickAction.entries[index - 1].group != action.group) Separator()
                Item(
                    label,
                    shortcut = if (action == QuickAction.GlobalSearch) AppShortcut.GlobalSearch.menuShortcut() else null,
                    onClick = { viewModel.run(action) },
                )
            }
            Separator()
            Item(
                openLabel,
                shortcut = AppShortcut.QuickActions.menuShortcut(),
                onClick = { controller.toggle(OpenOrigin.Keyboard) },
            )
            Item(
                shortcutsLabel,
                shortcut = AppShortcut.ShortcutsList.menuShortcut(),
                onClick = { controller.openShortcuts() },
            )
        }
    }
}

@Composable
fun QuickActionsTitleBarMenu() {
    val controller = koinInject<QuickActionsController>()
    // Not scoped to this menu: it resolves to the same ViewModelStoreOwner as QuickActionsOverlay,
    // so it shares the overlay's instance and a run started here shows the overlay's snackbar.
    val viewModel = koinViewModel<QuickActionsViewModel>()
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) { Text(stringResource(Res.string.qa_menu_actions)) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        QuickAction.entries.forEachIndexed { index, action ->
            if (index > 0 && QuickAction.entries[index - 1].group != action.group) HorizontalDivider()
            DropdownMenuItem(
                text = {
                    val label = stringResource(action.label())
                    Text(if (action == QuickAction.GlobalSearch) "$label  ${AppShortcut.GlobalSearch.label(false)}" else label)
                },
                onClick = {
                    expanded = false
                    viewModel.run(action)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.qa_menu_open_shortcut, AppShortcut.QuickActions.label(false))) },
            onClick = {
                expanded = false
                controller.open(OpenOrigin.Keyboard)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.qa_menu_shortcuts)) },
            onClick = {
                expanded = false
                controller.openShortcuts()
            },
        )
    }
}
