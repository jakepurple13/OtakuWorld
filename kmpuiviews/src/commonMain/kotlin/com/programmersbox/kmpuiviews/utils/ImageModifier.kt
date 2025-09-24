package com.programmersbox.kmpuiviews.utils

import androidx.compose.ui.graphics.ImageBitmap

expect class ImageModifier {
    fun close()
    suspend fun startImageDescription(
        bitmap: ImageBitmap,
    )
}