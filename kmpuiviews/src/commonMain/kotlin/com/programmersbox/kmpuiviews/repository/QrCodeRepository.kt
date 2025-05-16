package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.graphics.ImageBitmap

expect class QrCodeRepository {
    suspend fun getInfoFromQRCode(
        bitmap: ImageBitmap,
    ): Result<List<String>>

    suspend fun shareImage(bitmap: ImageBitmap, title: String)
    suspend fun saveImage(bitmap: ImageBitmap, title: String)
    suspend fun shareUrl(url: String, title: String)
}