package com.programmersbox.kmpuiviews.repository

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

actual class QrCodeRepository(
    private val context: Context,
) {
    val scanner = BarcodeScanning.getClient()
    actual suspend fun getInfoFromQRCode(
        bitmap: ImageBitmap,
    ): Result<List<String>> = runCatching { InputImage.fromBitmap(bitmap.asAndroidBitmap(), 0) }
        .mapCatching { scanner.process(it).await() }
        .mapCatching { barcodes ->
            barcodes.mapNotNull { it.displayValue }
        }
}
