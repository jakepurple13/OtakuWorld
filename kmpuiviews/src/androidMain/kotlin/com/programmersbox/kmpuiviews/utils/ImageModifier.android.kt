package com.programmersbox.kmpuiviews.utils

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap

//TODO: This will be for using ai or similar to detect if a cover is nsfw or not
actual class ImageModifier(
    context: Context,
) {

    actual suspend fun startImageDescription(
        bitmap: ImageBitmap,
    ) {
    }

    actual fun close() {

    }
}