package com.programmersbox.kmpuiviews.motion

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** How long [WindowPicture.capture] waits for the main window to redraw before using its last frame. */
private const val REDRAW_TIMEOUT_MILLIS = 250L

/**
 * A picture of the main window's content for the quit overlay. [recordInto] on the content keeps the last drawn frame
 * in a graphics layer of that window; [capture] turns it into a bitmap.
 */
class WindowPicture {
    internal var layer: GraphicsLayer? = null

    /** Bumped by [capture]; read while drawing, so a bump redraws the content. */
    internal var redrawRequest by mutableIntStateOf(0)

    /** The [redrawRequest] the content last recorded for. Written while drawing, never read there. */
    internal var recordedFor by mutableIntStateOf(0)

    /**
     * The content's next drawn frame (so it reflects state changed just before, such as a skipped launch motion), or
     * its last one if it does not redraw within [REDRAW_TIMEOUT_MILLIS]. Null before it has drawn (or if the layer
     * can't be read).
     */
    suspend fun capture(): ImageBitmap? {
        val request = ++redrawRequest
        withTimeoutOrNull(REDRAW_TIMEOUT_MILLIS) { snapshotFlow { recordedFor }.first { it >= request } }
        return runCatching { layer?.toImageBitmap() }.getOrNull()
    }
}

/**
 * Records what this content draws into [picture] (and draws it as usual). Must be applied inside the window whose
 * content it records: the layer belongs to that window's graphics context.
 */
fun Modifier.recordInto(picture: WindowPicture): Modifier = composed {
    val layer = rememberGraphicsLayer()
    SideEffect { picture.layer = layer }
    drawWithContent {
        val request = picture.redrawRequest
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
        picture.recordedFor = request
    }
}
