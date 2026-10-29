package com.programmersbox.kmpuiviews.utils.composables.sharedelements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.geometry.Offset
import org.koin.compose.koinInject

/**
 * Hands the cover a card already decoded to the Details screen it opens, so Details can show the
 * cover and build its palette right away instead of loading the image again.
 * Holds at most one bitmap, and only until Details takes it.
 */
class SharedCoverBitmaps {
    private var pending: Pair<SharedItemKey, ImageBitmap>? = null

    fun offer(key: SharedItemKey, bitmap: ImageBitmap) {
        pending = key to bitmap
    }

    fun take(key: SharedItemKey): ImageBitmap? {
        val bitmap = pending?.takeIf { it.first == key }?.second
        pending = null
        return bitmap
    }
}

/** Remembers the bitmap a card's image loaded so it can be handed to Details on tap */
class CoverHandoff internal constructor(
    private val key: SharedItemKey?,
    private val bitmaps: SharedCoverBitmaps,
) {
    private var loaded: ImageBitmap? = null

    val onImageSet: (ImageBitmap) -> Unit = { loaded = it }

    fun handOff() {
        val key = key ?: return
        // Copy it: the image loader may recycle its bitmap once the card leaves the screen
        loaded?.let { bitmaps.offer(key, it.copy()) }
    }
}

@Composable
fun rememberCoverHandoff(key: SharedItemKey?): CoverHandoff {
    val bitmaps = koinInject<SharedCoverBitmaps>()
    return remember(key, bitmaps) { CoverHandoff(key, bitmaps) }
}

private fun ImageBitmap.copy(): ImageBitmap {
    val copy = ImageBitmap(width, height)
    Canvas(copy).drawImage(this, Offset.Zero, Paint())
    return copy
}
