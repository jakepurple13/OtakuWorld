package com.programmersbox.sharedcomponents.qrcode

import androidx.compose.ui.graphics.ImageBitmap

actual class QrCodeRepository {
    actual suspend fun getInfoFromQRCode(bitmap: ImageBitmap): Result<List<String>> {
        TODO("Not yet implemented")
    }

    actual suspend fun shareImage(bitmap: ImageBitmap, title: String) {
    }

    actual suspend fun saveImage(bitmap: ImageBitmap, title: String) {
    }

    actual suspend fun shareUrl(url: String, title: String) {
    }
}