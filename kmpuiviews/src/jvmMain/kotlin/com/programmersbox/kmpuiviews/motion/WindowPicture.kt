package com.programmersbox.kmpuiviews.motion

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

/**
 * A picture of the main window's content for the quit overlay. [recordInto] on the content keeps the last drawn frame
 * in a graphics layer of that window; [capture] turns it into a bitmap.
 */
class WindowPicture {
    internal var layer: GraphicsLayer? = null

    /** The content's last drawn frame, or null before it has drawn (or if the layer can't be read). */
    suspend fun capture(): ImageBitmap? = runCatching { layer?.toImageBitmap() }.getOrNull()
}

/**
 * Records what this content draws into [picture] (and draws it as usual). Must be applied inside the window whose
 * content it records: the layer belongs to that window's graphics context.
 */
fun Modifier.recordInto(picture: WindowPicture): Modifier = composed {
    val layer = rememberGraphicsLayer()
    SideEffect { picture.layer = layer }
    drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }
}
