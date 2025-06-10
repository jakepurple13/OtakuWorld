package com.programmersbox.kmpuiviews.repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.awt.image.BufferedImage


actual class QrCodeRepository {
    private val reader: QRCodeReader by lazy { QRCodeReader() }

    actual suspend fun getInfoFromQRCode(
        bitmap: ImageBitmap,
    ): Result<List<String>> = runCatching {

        // 1. Load the image
        val image: BufferedImage? = bitmap.toAwtImage()

        // 3. Read the barcode
        val result = reader.decode(
            BinaryBitmap(
                HybridBinarizer(BufferedImageLuminanceSource(image))
            )
        )

        // 4. Return the decoded text
        listOf(result.text)
    }

    actual suspend fun shareImage(bitmap: ImageBitmap, title: String) {

    }

    actual suspend fun saveImage(bitmap: ImageBitmap, title: String) {

    }

    actual suspend fun shareUrl(url: String, title: String) {

    }
}