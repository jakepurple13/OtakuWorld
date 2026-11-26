package com.programmersbox.kmpuiviews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.tabs.BrowserWindow
import com.programmersbox.kmpuiviews.tabs.BrowserWindowState
import com.programmersbox.kmpuiviews.tabs.MiniPlayerOverlay
import com.programmersbox.kmpuiviews.tabs.RegisterWindowGeometry
import com.programmersbox.kmpuiviews.tabs.TabDragLayer
import com.programmersbox.kmpuiviews.tabs.TabKeys
import com.programmersbox.kmpuiviews.tabs.TabMotion
import com.programmersbox.kmpuiviews.tabs.TabMotionLayer
import com.programmersbox.kmpuiviews.tabs.TabPosition
import com.programmersbox.kmpuiviews.tabs.TabStrip
import com.programmersbox.kmpuiviews.tabs.TabsController
import com.programmersbox.kmpuiviews.tabs.TearOutOverlay
import com.programmersbox.kmpuiviews.tabs.TearOutStyle
import com.programmersbox.kmpuiviews.tabs.TrackWindowBounds
import com.programmersbox.kmpuiviews.tabs.chromeVisible
import com.programmersbox.kmpuiviews.tabs.closeWindow
import com.programmersbox.kmpuiviews.tabs.lift
import com.programmersbox.kmpuiviews.tabs.perform
import com.programmersbox.kmpuiviews.tabs.rememberLift
import com.programmersbox.kmpuiviews.tabs.tabShortcut
import dev.nucleusframework.window.TitleBarScope
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** How long past its motion a tear-out overlay may take before its window shows anyway. */
private const val OverlayGraceMs = 1_000L

/** A window's title bar: its tab strip when tabs are on top, otherwise [title]. */
@Composable
internal fun TitleBarScope.WindowTitleBar(
    window: BrowserWindowState,
    controller: TabsController,
    title: String,
) {
    // On the side, the tabs live in BrowserWindow's column and the title bar shows the title. Onboarding hides them.
    if (window.chromeVisible && controller.motion.position == TabPosition.Top) {
        TabStrip(
            window = window,
            controller = controller,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/**
 * A window's tab shortcuts at the window level. With focus inside the content, ⌘← / ⌘→ are left to the content (a
 * text field uses them to move the caret) and handled when they bubble up to BrowserWindow.
 */
internal fun windowTabShortcut(event: KeyEvent, window: BrowserWindowState, controller: TabsController): Boolean =
    tabShortcut(
        event,
        isMac = isMacOs,
        chromeVisible = window.chromeVisible,
        textFieldFocused = controller.ui.contentHasFocus[window.id] == true,
    )?.let { controller.perform(it, window) } != null

/** What every window draws over its content: tab motion, drag feedback and the mini player. */
@Composable
internal fun WindowOverlays(window: BrowserWindowState, controller: TabsController) {
    RegisterWindowGeometry(controller, window.id)
    TabMotionLayer(
        controller = controller,
        window = window,
        tabSlotBounds = { controller.motion.slotBounds[it] },
        contentBounds = { controller.motion.contentBounds[window.id] ?: Rect.Zero },
    )
    TabDragLayer(controller = controller, window = window)
    MiniPlayerOverlay(registry = koinInject(), window = window)
}

/** Every torn-out window. The main window is drawn by the app shell. */
@Composable
internal fun TornOutWindows(controller: TabsController, appColors: ColorScheme?) {
    controller.windows.filter { !it.isMain }.forEach { window ->
        key(window.id.v) { TornOutWindow(window, controller, appColors) }
    }
}

/**
 * A torn-out window: the same shell as the main window (strip on top or side, toolbar, content) without Home. Its
 * title is the active tab's label. Closing it closes its tabs, which go to Recently closed.
 */
@Composable
private fun TornOutWindow(
    window: BrowserWindowState,
    controller: TabsController,
    // The app theme, read from the main window: this runs outside any window, where MaterialTheme is the default.
    appColors: ColorScheme?,
    keys: TabKeys = koinInject(),
) {
    val bounds = window.bounds.value ?: TabsController.DefaultWindowBounds
    val state = rememberWindowState(
        position = WindowPosition(bounds.x, bounds.y),
        size = DpSize(bounds.width, bounds.height),
    )
    TrackWindowBounds(window, state)
    // The controller moves the window too: tearing out the only tab of a torn-out window just moves it.
    LaunchedEffect(window, state) {
        snapshotFlow { window.bounds.value }.collect { b ->
            if (b == null || state.placement != WindowPlacement.Floating) return@collect
            val position = WindowPosition(b.x, b.y)
            val size = DpSize(b.width, b.height)
            if (state.position != position) state.position = position
            if (state.size != size) state.size = size
        }
    }

    val run = controller.ui.tearOuts[window.id]
    if (run != null && run.style == TearOutStyle.Overlay) {
        // Never left hidden: if the overlay can't draw, the window shows once its motion would have ended.
        LaunchedEffect(run) {
            delay(controller.motion.ms(TabMotion.TearOut) + OverlayGraceMs)
            run.shown = true
        }
        TearOutOverlay(
            from = run.from,
            to = bounds,
            color = (appColors ?: MaterialTheme.colorScheme).surface,
            durationMs = controller.motion.ms(TabMotion.TearOut),
            onDone = { run.shown = true },
        )
    }

    val active = window.tabs.firstOrNull { it.id == window.activeId.value } ?: window.tabs.firstOrNull()
    val title = active?.let { keys.labelOf(it.current).text }.orEmpty()
    OtakuDecoratedWindow(
        title = title,
        onCloseRequest = { controller.closeWindow(window.id) },
        state = state,
        visible = run?.shown ?: true,
        showQuickActions = true,
        hostsOverlays = controller.focusedWindow == window.id,
        titleBarContent = { WindowTitleBar(window, controller, title) },
        onKeyEvent = { windowTabShortcut(it, window, controller) },
        overlay = { WindowOverlays(window, controller) },
    ) {
        if (run != null && run.style == TearOutStyle.Overlay && run.shown) {
            // The window has drawn its first frame once the next one starts; then the overlay can go.
            LaunchedEffect(run) {
                withFrameNanos { }
                withFrameNanos { }
                controller.ui.tearOuts.remove(window.id)
            }
        }
        val lift = rememberLift(run, controller.motion.ms(TabMotion.Lift)) { controller.ui.tearOuts.remove(window.id) }
        Box(Modifier.fillMaxSize().lift(lift)) {
            BrowserWindow(window = window, controller = controller)
        }
    }
}

/**
 * Asks before quitting with several tabs open. [onQuit] quits; [onCancel] stays (and answers a held OS quit with
 * "cancel"). "Don't ask again" turns the setting off when quitting.
 */
@Composable
internal fun ConfirmQuitDialog(tabCount: Int, onQuit: (dontAskAgain: Boolean) -> Unit, onCancel: () -> Unit) {
    var dontAsk by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Quit with $tabCount tabs open?") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                Text("Don't ask again")
            }
        },
        confirmButton = { TextButton(onClick = { onQuit(dontAsk) }) { Text("Quit") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
