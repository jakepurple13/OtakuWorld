package com.programmersbox.kmpuiviews.motion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop

/**
 * Transparent, undecorated, non-focusable, always-on-top window at [position]/[size]. Draws a [titleBarHeight] band of
 * [titleBarColor] above [picture] (or a [surfaceColor] rectangle when there is no picture), wrapped in
 * MotionHost(run.motion). Calls [onFirstFrame] once its first frame is drawn, [onEnd] after [run] ends.
 */
@Composable
fun QuitOverlayWindow(
    position: WindowPosition,
    size: DpSize,
    picture: ImageBitmap?,
    titleBarHeight: Dp,
    titleBarColor: Color,
    surfaceColor: Color,
    run: MotionRun,
    onFirstFrame: () -> Unit,
    onEnd: () -> Unit,
) {
    val firstFrame by rememberUpdatedState(onFirstFrame)
    Window(
        onCloseRequest = {},
        state = rememberWindowState(position = position, size = size),
        title = "",
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            // The first frame has been drawn once the next one starts.
            withFrameNanos { }
            firstFrame()
        }
        run.Play(onEnd)
        MotionHost(motion = run.motion, time = { run.timeMillis }, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(titleBarHeight).background(titleBarColor))
                if (picture != null) {
                    Image(picture, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                } else {
                    Box(Modifier.fillMaxSize().background(surfaceColor))
                }
            }
        }
    }
}

/**
 * The quit overlay for [controller] while it plays with [QuitHost.Overlay]: takes [picture] of the main window (at
 * [windowState]), then shows [QuitOverlayWindow] over it. [contentHeight] is the recorded content's height, so the
 * band above it matches the main window's title bar; it is a lambda so resizes don't recompose the caller.
 */
@Composable
fun QuitOverlay(
    controller: QuitController,
    picture: WindowPicture,
    windowState: WindowState,
    contentHeight: () -> Dp,
    titleBarColor: Color,
    surfaceColor: Color,
) {
    val playing = controller.state as? QuitController.State.Playing ?: return
    if (playing.plan.host != QuitHost.Overlay) return
    var captured by remember(playing) { mutableStateOf(false) }
    var image by remember(playing) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(playing) {
        // Waits for the main window's next frame, so a launch motion skipped by this quit is not in the picture.
        image = picture.capture()
        captured = true
    }
    if (!captured) return
    QuitOverlayWindow(
        position = windowState.position,
        size = windowState.size,
        picture = image,
        titleBarHeight = titleBarBand(windowState.size.height, contentHeight()),
        titleBarColor = titleBarColor,
        surfaceColor = surfaceColor,
        run = playing.run,
        onFirstFrame = controller::overlayFirstFrame,
        onEnd = controller::finished,
    )
}

/** Height of the band above the content: what the window has beyond its content, never negative. */
fun titleBarBand(windowHeight: Dp, contentHeight: Dp): Dp =
    if (!windowHeight.isSpecified || !contentHeight.isSpecified) 0.dp
    else (windowHeight - contentHeight).coerceAtLeast(0.dp)

/**
 * Routes the app's Quit (⌘Q, Dock, logout on macOS) to [onQuit] instead of exiting at once, where the platform supports
 * it. The OS's answer is deferred into [reply]; the app loop's caller answers it with
 * [PendingQuitReply.releaseAfter] once the composition is disposed. It is refused ([PendingQuitReply.cancel]) only
 * when the user cancels the confirm-quit dialog.
 */
@Composable
fun AppQuitHandler(reply: PendingQuitReply, onQuit: () -> Unit) {
    val quit by rememberUpdatedState(onQuit)
    DisposableEffect(reply) {
        val desktop = Desktop.isDesktopSupported()
            .takeIf { it }
            ?.let { Desktop.getDesktop() }
            ?.takeIf { it.isSupported(Desktop.Action.APP_QUIT_HANDLER) }
        desktop?.setQuitHandler { _, response ->
            reply.hold(response::performQuit, response::cancelQuit)
            quit()
        }
        onDispose { desktop?.setQuitHandler(null) }
    }
}
