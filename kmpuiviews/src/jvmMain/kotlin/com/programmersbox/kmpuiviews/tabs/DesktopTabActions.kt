package com.programmersbox.kmpuiviews.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberCursorPositionProvider
import com.programmersbox.kmpuiviews.presentation.quickactions.isMacOs
import com.programmersbox.kmpuiviews.presentation.tabs.TabActions
import org.jetbrains.compose.resources.stringResource
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.tabs_more
import otakuworld.kmpuiviews.generated.resources.tabs_open
import otakuworld.kmpuiviews.generated.resources.tabs_open_in_new_tab
import otakuworld.kmpuiviews.generated.resources.tabs_open_in_new_tab_and_switch
import otakuworld.kmpuiviews.generated.resources.tabs_open_in_new_window

/**
 * The mode a press asks for: ⌘ (Ctrl off macOS) opens in the background, adding ⇧ switches to it,
 * and a middle press opens in the background. Null means a plain press.
 */
fun modeFor(isMac: Boolean, meta: Boolean, ctrl: Boolean, shift: Boolean, button: PointerButton): TabOpenMode? =
    when (button) {
        PointerButton.Tertiary -> TabOpenMode.Background
        PointerButton.Primary -> {
            val primary = if (isMac) meta && !ctrl else ctrl && !meta
            when {
                !primary -> null
                shift -> TabOpenMode.Foreground
                else -> TabOpenMode.Background
            }
        }

        else -> null
    }

/**
 * [TabActions] for one tab. Presses arm [controller] so the element's own click opens a new tab;
 * right-click opens [Menu], which the tab shell composes once per tab.
 */
class DesktopTabActions(
    private val controller: TabsController,
    private val tabId: TabId,
    private val isMac: Boolean = isMacOs,
) : TabActions {

    private class MenuRequest(val origin: Rect?, val onOpen: () -> Unit, val onMore: (() -> Unit)?)

    private var menu by mutableStateOf<MenuRequest?>(null)

    private class Bounds(var rect: Rect? = null)

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Modifier.target(onOpen: () -> Unit, onMore: (() -> Unit)?): Modifier {
        val currentOpen by rememberUpdatedState(onOpen)
        val currentMore by rememberUpdatedState(onMore)
        val bounds = remember { Bounds() }
        return this
            .onGloballyPositioned { bounds.rect = it.boundsInWindow() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Initial pass and not consumed: the element's own click still runs, and navigates.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Press) continue
                        val button = event.button ?: continue
                        if (button == PointerButton.Secondary) {
                            menu = MenuRequest(bounds.rect, currentOpen, currentMore)
                            continue
                        }
                        val mods = event.keyboardModifiers
                        val mode = modeFor(isMac, mods.isMetaPressed, mods.isCtrlPressed, mods.isShiftPressed, button)
                            ?: continue
                        controller.arm(tabId, mode, bounds.rect)
                        // Clickables ignore the middle button, so open here.
                        if (button == PointerButton.Tertiary) currentOpen()
                    }
                }
            }
    }

    /** The right-click menu, at the cursor. */
    @Composable
    fun Menu() {
        val request = menu ?: return
        fun pick(mode: TabOpenMode?) {
            menu = null
            if (mode != null) controller.arm(tabId, mode, request.origin)
            request.onOpen()
        }

        Popup(
            popupPositionProvider = rememberCursorPositionProvider(),
            onDismissRequest = { menu = null },
            properties = PopupProperties(focusable = true),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.width(IntrinsicSize.Max).padding(vertical = 8.dp)) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.tabs_open)) }, onClick = { pick(null) })
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.tabs_open_in_new_tab)) },
                        onClick = { pick(TabOpenMode.Background) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.tabs_open_in_new_tab_and_switch)) },
                        onClick = { pick(TabOpenMode.Foreground) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.tabs_open_in_new_window)) },
                        onClick = { pick(TabOpenMode.NewWindow) },
                    )
                    request.onMore?.let { onMore ->
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.tabs_more)) },
                            onClick = {
                                menu = null
                                onMore()
                            },
                        )
                    }
                }
            }
        }
    }
}
